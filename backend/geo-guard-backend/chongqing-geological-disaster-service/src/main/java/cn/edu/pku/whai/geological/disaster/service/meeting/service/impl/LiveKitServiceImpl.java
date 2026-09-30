/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.service.impl;

import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.meeting.cache.MeetingRedisCache;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.req.LiveKitTokenReq;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.resp.LiveKitTokenResp;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.vo.MeetingInfoVo;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.vo.MeetingParticipantsStatus;
import cn.edu.pku.whai.geological.disaster.service.meeting.props.LiveKitProps;
import cn.edu.pku.whai.geological.disaster.service.meeting.service.ILiveKitService;
import cn.hutool.core.util.StrUtil;
import io.livekit.server.*;
import jakarta.servlet.http.HttpServletRequest;
import livekit.LivekitWebhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveKitServiceImpl implements ILiveKitService {

    private static final String EVENT_PARTICIPANT_JOINED = "participant_joined";
    private static final String EVENT_PARTICIPANT_LEFT = "participant_left";

    private final LiveKitProps liveKitProps;

    @Override
    public LiveKitTokenResp generateToken(LiveKitTokenReq req) {
        validateConfig();
        if (req == null || req.getMeetingId() == null) {
            throw new ServiceException("会议ID不能为空");
        }

        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new ServiceException("未获取到当前登录用户");
        }
        validateMeetingPermission(req.getMeetingId(), loginUser.getUserId());

        String roomName = req.getMeetingId().toString();
        AccessToken token = new AccessToken(liveKitProps.getApiKey(), liveKitProps.getApiSecret());
        token.setIdentity(loginUser.getUserId().toString());
        token.setName(resolveParticipantName(loginUser));
        token.setTtl(TimeUnit.MILLISECONDS.convert(resolveTtlSeconds(), TimeUnit.SECONDS));
        token.addGrants(
            new RoomJoin(true),
            new RoomName(roomName),
            new CanPublish(true),
            new CanSubscribe(true),
            new CanPublishData(true)
        );

        LiveKitTokenResp resp = new LiveKitTokenResp();
        resp.setUrl(resolveLiveKitUrl(ServletUtils.getRequest()));
        resp.setRoomName(roomName);
        resp.setToken(token.toJwt());
        return resp;
    }

    @Override
    public void closeRoom(Long meetingId) {
        validateConfig();
        if (meetingId == null) {
            throw new ServiceException("会议ID不能为空");
        }

        Response<Void> response = execute(() -> createClient().deleteRoom(meetingId.toString()).execute(), "关闭LiveKit房间失败");
        if (response.isSuccessful()) {
            return;
        }
        if (response.code() == 404) {
            log.warn("LiveKit room already closed or not found, meetingId: {}", meetingId);
            return;
        }
        throwLiveKitResponseException(response, "关闭LiveKit房间失败");
    }

    @Override
    public void removeParticipant(Long meetingId, Long userId) {
        validateConfig();
        if (meetingId == null) {
            throw new ServiceException("会议ID不能为空");
        }
        if (userId == null) {
            throw new ServiceException("用户ID不能为空");
        }

        Response<Void> response = execute(
            () -> createClient().removeParticipant(meetingId.toString(), userId.toString()).execute(),
            "移除LiveKit参与者失败"
        );
        if (response.isSuccessful()) {
            return;
        }
        if (response.code() == 404) {
            log.warn("LiveKit participant or room not found, meetingId: {}, userId: {}", meetingId, userId);
            return;
        }
        throwLiveKitResponseException(response, "移除LiveKit参与者失败");
    }

    @Override
    public void handleWebhook(String body, String authorization) {
        validateConfig();
        if (StrUtil.isBlank(body)) {
            throw new ServiceException("LiveKit webhook内容不能为空");
        }

        LivekitWebhook.WebhookEvent event = receiveWebhook(body, authorization);
        if (!EVENT_PARTICIPANT_JOINED.equals(event.getEvent()) && !EVENT_PARTICIPANT_LEFT.equals(event.getEvent())) {
            return;
        }
        if (!event.hasRoom() || !event.hasParticipant()) {
            return;
        }

        Long meetingId = parseMeetingId(event.getRoom().getName());
        Long userId = parseUserId(event.getParticipant().getIdentity());
        if (meetingId == null || userId == null) {
            return;
        }

        Integer statusValue = EVENT_PARTICIPANT_JOINED.equals(event.getEvent()) ? 1 : 0;
        syncParticipantStatus(meetingId, userId, event.getParticipant().getName(), statusValue);
    }

    private void validateMeetingPermission(Long meetingId, Long userId) {
        MeetingInfoVo meetingInfo = MeetingRedisCache.getMeetingInfo(meetingId);
        if (meetingInfo == null) {
            throw new ServiceException("未发现当前会议");
        }
        if (Objects.equals(userId, meetingInfo.getInitiator())) {
            return;
        }
        if (meetingInfo.getParticipantsMap() == null || !meetingInfo.getParticipantsMap().containsKey(userId)) {
            throw new ServiceException("当前用户不在会议参与人列表中");
        }
    }

    private String resolveParticipantName(LoginUser loginUser) {
        if (StrUtil.isNotBlank(loginUser.getNickname())) {
            return loginUser.getNickname();
        }
        if (StrUtil.isNotBlank(loginUser.getUsername())) {
            return loginUser.getUsername();
        }
        return loginUser.getUserId().toString();
    }

    private long resolveTtlSeconds() {
        Long ttlSeconds = liveKitProps.getTokenTtlSeconds();
        if (ttlSeconds == null || ttlSeconds <= 0) {
            return 21600L;
        }
        return ttlSeconds;
    }

    private String resolveLiveKitUrl(HttpServletRequest request) {
        return resolveLiveKitUrl(getClientIp(request), isProdProfile());
    }

    String resolveLiveKitUrl(String clientIp, boolean prodProfile) {
        log.info("Resolving LiveKit url, prodProfile={}, clientIp={}", prodProfile, clientIp);
        if (!prodProfile) {
            return safeUrl(liveKitProps.getInnerUrl(), liveKitProps.getUrl());
        }
        if (isInnerIp(clientIp)) {
            return safeUrl(liveKitProps.getInnerUrl(), liveKitProps.getUrl());
        }
        return safeUrl(liveKitProps.getOuterUrl(), liveKitProps.getUrl());
    }

    private boolean isProdProfile() {
        return SpringUtils.getActiveProfile().equals("prod");
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String ip = firstNotBlank(
            request.getHeader("X-Forwarded-For"),
            request.getHeader("X-Real-IP"),
            request.getHeader("Proxy-Client-IP"),
            request.getHeader("WL-Proxy-Client-IP"),
            request.getHeader("HTTP_CLIENT_IP"),
            request.getHeader("HTTP_X_FORWARDED_FOR"),
            ServletUtils.getClientIP()
        );
        if (StrUtil.isNotBlank(ip)) {
            return normalizeIp(ip.split(",")[0].trim());
        }
        return normalizeIp(request.getRemoteAddr());
    }

    private boolean isInnerIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        String normalizedIp = normalizeIp(ip);
        if (StrUtil.equals(normalizedIp, "127.0.0.1") || StrUtil.equalsIgnoreCase(normalizedIp, "localhost")) {
            return true;
        }
        List<String> cidrs = liveKitProps.getInnerCidrs();
        if (cidrs == null || cidrs.isEmpty()) {
            cidrs = Arrays.asList("127.0.0.1/16", "127.0.0.1/8", "127.0.0.1/16");
        }
        for (String cidr : cidrs) {
            if (ipv4InCidr(normalizedIp, cidr)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeIp(String ip) {
        if (ip == null) {
            return "";
        }
        String normalized = ip.trim();
        if (StrUtil.equals(normalized, "0:0:0:0:0:0:0:1") || StrUtil.equals(normalized, "::1")) {
            return "127.0.0.1";
        }
        if (StrUtil.startWith(normalized, "::ffff:")) {
            return normalized.substring("::ffff:".length());
        }
        return normalized;
    }

    private boolean ipv4InCidr(String ip, String cidr) {
        try {
            if (StrUtil.isBlank(ip) || StrUtil.isBlank(cidr)) {
                return false;
            }
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return StrUtil.equals(ip, cidr);
            }
            long ipLong = ipv4ToLong(ip);
            long networkLong = ipv4ToLong(parts[0]);
            int prefix = Integer.parseInt(parts[1]);
            if (prefix < 0 || prefix > 32) {
                return false;
            }
            long mask = prefix == 0 ? 0 : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
            return (ipLong & mask) == (networkLong & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private long ipv4ToLong(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4: " + ip);
        }
        long result = 0;
        for (String part : parts) {
            int value = Integer.parseInt(part);
            if (value < 0 || value > 255) {
                throw new IllegalArgumentException("Invalid IPv4: " + ip);
            }
            result = (result << 8) + value;
        }
        return result;
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String safeUrl(String primary, String fallback) {
        if (StrUtil.isNotBlank(primary)) {
            return primary;
        }
        if (StrUtil.isNotBlank(fallback)) {
            return fallback;
        }
        throw new ServiceException("LiveKit连接地址未配置");
    }

    private void validateConfig() {
        if (StrUtil.isBlank(liveKitProps.getUrl())
            || StrUtil.isBlank(liveKitProps.getApiKey())
            || StrUtil.isBlank(liveKitProps.getApiSecret())) {
            throw new ServiceException("LiveKit配置不完整");
        }
    }

    private RoomServiceClient createClient() {
        return RoomServiceClient.create(liveKitProps.getUrl(), liveKitProps.getApiKey(), liveKitProps.getApiSecret());
    }

    private LivekitWebhook.WebhookEvent receiveWebhook(String body, String authorization) {
        try {
            return new WebhookReceiver(liveKitProps.getApiKey(), liveKitProps.getApiSecret())
                .receive(body, normalizeAuthorization(authorization));
        } catch (Exception e) {
            throw new ServiceException("LiveKit webhook校验失败");
        }
    }

    private String normalizeAuthorization(String authorization) {
        if (StrUtil.isBlank(authorization)) {
            return authorization;
        }
        String bearerPrefix = "Bearer ";
        if (authorization.regionMatches(true, 0, bearerPrefix, 0, bearerPrefix.length())) {
            return authorization.substring(bearerPrefix.length());
        }
        return authorization;
    }

    private Long parseMeetingId(String roomName) {
        if (StrUtil.isBlank(roomName)) {
            return null;
        }
        try {
            return Long.valueOf(roomName);
        } catch (NumberFormatException e) {
            log.warn("LiveKit webhook room name is not a meeting room, roomName: {}", roomName);
            return null;
        }
    }

    private Long parseUserId(String identity) {
        if (StrUtil.isBlank(identity)) {
            return null;
        }
        try {
            return Long.valueOf(identity);
        } catch (NumberFormatException e) {
            log.warn("LiveKit webhook participant identity is not user id, identity: {}", identity);
            return null;
        }
    }

    private void syncParticipantStatus(Long meetingId, Long userId, String participantName, Integer statusValue) {
        MeetingInfoVo meetingInfo = MeetingRedisCache.getMeetingInfo(meetingId);
        if (meetingInfo == null) {
            log.warn("LiveKit webhook meeting not found, meetingId: {}", meetingId);
            return;
        }
        Map<Long, MeetingParticipantsStatus> participantsMap = meetingInfo.getParticipantsMap();
        if (participantsMap == null) {
            participantsMap = new HashMap<>();
            meetingInfo.setParticipantsMap(participantsMap);
        }

        MeetingParticipantsStatus status = participantsMap.get(userId);
        if (status == null && statusValue == 0) {
            return;
        }
        if (status == null) {
            status = new MeetingParticipantsStatus();
            status.setUserId(userId);
            status.setNickName(StrUtil.blankToDefault(participantName, userId.toString()));
            participantsMap.put(userId, status);
        }
        status.setStatus(statusValue);
        MeetingRedisCache.addMeetingInfo(meetingId, meetingInfo);
    }

    private <T> Response<T> execute(LiveKitCall<T> call, String message) {
        try {
            return call.execute();
        } catch (IOException e) {
            throw new ServiceException(message);
        }
    }

    private void throwLiveKitResponseException(Response<?> response, String message) {
        log.warn("{}，responseCode: {}", message, response.code());
        throw new ServiceException(message);
    }

    @FunctionalInterface
    private interface LiveKitCall<T> {
        Response<T> execute() throws IOException;
    }
}
