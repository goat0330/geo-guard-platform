/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.data.utils.LevelCodeUtil;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.*;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.StartMeetingBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.*;
import cn.edu.pku.whai.geological.disaster.service.mapper.*;
import cn.edu.pku.whai.geological.disaster.service.meeting.cache.MeetingRedisCache;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.bo.CallParticipantsBo;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.req.LiveKitTokenReq;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.resp.MeetingJoinResp;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.vo.MeetingInfoVo;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.vo.MeetingParticipantsStatus;
import cn.edu.pku.whai.geological.disaster.service.meeting.service.ILiveKitService;
import cn.edu.pku.whai.geological.disaster.service.meeting.service.IMeetingService;
import cn.edu.pku.whai.geological.disaster.service.meeting.service.MeetingSmsSender;
import cn.edu.pku.whai.geological.disaster.service.sse.SseEmitterManager;
import cn.edu.pku.whai.geological.disaster.service.sse.domain.resp.SseResp;
import cn.edu.pku.whai.geological.disaster.service.utils.DefRespPlanCodeUtil;
import org.dromara.system.domain.SysUser;
import org.dromara.system.mapper.SysUserMapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IMeetingServiceImpl implements IMeetingService {

    private static final String MEETING_JOIN_TYPE = "meeting-join";
    private static final String MEETING_INVITE_TITLE = "专家会商邀请";
    private static final int DEF_RESP_MEETING_TYPE = 2;
    private static final long DEF_RESP_PLACEHOLDER_HANDLE_ID = 10000000L;
    private static final int DEF_RESP_PLACEHOLDER_PROCESS = 1;
    private static final int DEF_RESP_MEETING_TYPE_A = 4;

    private final DzDefRespPlanMapper dzDefRespPlanMapper;
    private final DzTaskHandleMapper dzTaskHandleMapper;
    private final SysUserMapper sysUserMapper;
    private final SseEmitterManager sseEmitterManager;
    private final DzMsgNoticeMapper dzMsgNoticeMapper;
    private final DzRiskPredictionPushMapper dzRiskPredictionPushMapper;
    private final ILiveKitService liveKitService;
    private final Optional<MeetingSmsSender> meetingSmsSender;

    @Override
    public MeetingInfoVo startMeeting(StartMeetingBo bo) {
        Long meetingId = MeetingRedisCache.getHandleProcess(bo.getHandleId(), bo.getHandleProcess());
        if (meetingId != null) {
            return MeetingRedisCache.getMeetingInfo(meetingId);
        }

        meetingId = IdUtil.getSnowflakeNextId();
        CountyDefRespMeetingIdentity countyIdentity = resolveCountyDefRespMeetingIdentity(bo);
        Long handleId = countyIdentity != null ? countyIdentity.handleId() : bo.getHandleId();
        String planCode = countyIdentity != null ? countyIdentity.planCode() : null;
        String initiatorName = LoginHelper.getLoginUser().getNickname();
        List<Long> userIds = normalizeUserIds(bo.getParticipants());

        MeetingInfoVo vo = buildMeetingInfo(bo, meetingId, handleId, initiatorName, userIds, planCode);
        String content = buildInviteContent(bo.getMeetingType(), handleId, initiatorName, planCode);
        MeetingRedisCache.addHandleProcess(bo.getHandleId(), bo.getHandleProcess(), meetingId);
        MeetingRedisCache.addMeetingInfo(meetingId, vo);
        if (CollUtil.isEmpty(userIds)) {
            return vo;
        }
        sendMeetingJoinNotices(userIds, vo, content);
        return vo;
    }

    private CountyDefRespMeetingIdentity resolveCountyDefRespMeetingIdentity(StartMeetingBo bo) {
        if (!isCountyDefRespPlaceholderMeeting(bo)) {
            return null;
        }
        DefRespPlan countyPlan = resolveExistingCountyDefRespPlan();
        if (countyPlan != null) {
            Long handleId = countyPlan.getHandleId() != null ? countyPlan.getHandleId() : countyPlan.getId();
            return new CountyDefRespMeetingIdentity(handleId, requirePlanCode(countyPlan));
        }
        return new CountyDefRespMeetingIdentity(IdUtil.getSnowflakeNextId(), DefRespPlanCodeUtil.buildCode(DefRespPlanCodeUtil.MARKER_COUNTY));
    }

    private boolean isCountyDefRespPlaceholderMeeting(StartMeetingBo bo) {
        if (bo == null) {
            return false;
        }
        return (Objects.equals(bo.getMeetingType(), DEF_RESP_MEETING_TYPE) || Objects.equals(bo.getMeetingType(), DEF_RESP_MEETING_TYPE_A))
            && Objects.equals(bo.getHandleId(), DEF_RESP_PLACEHOLDER_HANDLE_ID)
            && Objects.equals(bo.getHandleProcess(), DEF_RESP_PLACEHOLDER_PROCESS);
    }

    private DefRespPlan resolveExistingCountyDefRespPlan() {
        return dzDefRespPlanMapper.selectOne(
            Wrappers.<DefRespPlan>lambdaQuery()
                    .eq(DefRespPlan::getRegionScopeType, RegionScopeTypeEnum.COUNTY.getCode())
                    .in(DefRespPlan::getStatus, DefRespPlanStatusEnum.getStartedStatusCodes())
                    .eq(DefRespPlan::getDeleted, 0)
                    .orderByDesc(DefRespPlan::getUpdateDate, DefRespPlan::getId)
                    .last("limit 1")
        );
    }

    private record CountyDefRespMeetingIdentity(Long handleId, String planCode) {
    }


    @Override
    public MeetingInfoVo getMeetingInfo(Long meetingId) {
        return MeetingRedisCache.getMeetingInfo(meetingId);
    }

    @Override
    public void closeMeeting(Long meetingId) {
        MeetingInfoVo meetingInfo = requireMeeting(meetingId, "会议结束或者会议不存在");
//        requireInitiator(meetingInfo, "只有主持人才能关闭会议");
        liveKitService.closeRoom(meetingId);
        MeetingRedisCache.closeMeeting(meetingId);
    }

    @Override
    public void exitMeeting(Long meetingId) {
        MeetingInfoVo meetingInfo = requireMeeting(meetingId);
        updateParticipantStatus(meetingInfo, LoginHelper.getUserId(), 0);
        MeetingRedisCache.addMeetingInfo(meetingId, meetingInfo);
    }

    @Override
    public Long getMeetingId(Long id, Integer process) {
        return MeetingRedisCache.getHandleProcess(id, process);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public MeetingJoinResp joinMeeting(Long meetingId) {
        Long userId = LoginHelper.getUserId();
        String nickname = LoginHelper.getLoginUser().getNickname();

        MeetingInfoVo meetingInfo = requireMeeting(meetingId);
        Map<Long, MeetingParticipantsStatus> participantsMap = requireParticipantsMap(meetingInfo);
        MeetingParticipantsStatus status = participantsMap.get(userId);
        if (status == null && isInitiator(meetingInfo, userId)) {
            throw new ServiceException("当前用户不在会议参与人列表中");
        }

        if (status == null) {
            status = buildParticipantStatus(userId, nickname, 0);
            participantsMap.put(userId, status);
        }
        status.setStatus(1);
        MeetingRedisCache.addMeetingInfo(meetingId, meetingInfo);

        return buildJoinResp(meetingInfo, meetingId);
    }

    @Override
    public void recallParticipants(CallParticipantsBo bo) {
        Long meetingId = bo.getMeetingId();
        MeetingInfoVo vo = requireMeeting(meetingId);
        sendMeetingJoinSse(bo.getUserIds(), vo);
    }

    @Override
    public void removeParticipants(CallParticipantsBo bo) {
        Long meetingId = bo.getMeetingId();
        MeetingInfoVo vo = requireMeeting(meetingId);
        requireInitiator(vo, "只有主持人才能移除参与者");
        Map<Long, MeetingParticipantsStatus> map = requireParticipantsMap(vo);
        for (Long userId : bo.getUserIds()) {
            liveKitService.removeParticipant(meetingId, userId);
            map.remove(userId);
        }

        MeetingRedisCache.addMeetingInfo(meetingId, vo);
    }

    @Override
    public void closeDefRespMeetingsOnArchive(Long defRespPlanId, Long currentHandleId) {
        Set<Long> meetingIdsToClose = new LinkedHashSet<>();
        Set<Long> handleIdsToScan = new LinkedHashSet<>();
        handleIdsToScan.add(DEF_RESP_PLACEHOLDER_HANDLE_ID);
        if (currentHandleId != null) {
            handleIdsToScan.add(currentHandleId);
        }
        if (defRespPlanId != null) {
            handleIdsToScan.add(defRespPlanId);
        }
        for (Long handleId : handleIdsToScan) {
            meetingIdsToClose.addAll(MeetingRedisCache.listMeetingIdsByHandleProcessKey(handleId));
            meetingIdsToClose.addAll(MeetingRedisCache.listMeetingIdsByHandleId(handleId));
        }
        for (Long meetingId : meetingIdsToClose) {
            if (meetingId == null || MeetingRedisCache.getMeetingInfo(meetingId) == null) {
                continue;
            }
            try {
                closeMeeting(meetingId);
                log.info("防御响应归档关闭会议成功 meetingId={}", meetingId);
            } catch (Exception e) {
                log.warn("防御响应归档关闭会议失败 meetingId={}", meetingId, e);
            }
        }
    }

    @Override
    public void addParticipants(CallParticipantsBo bo) {
        Long meetingId = bo.getMeetingId();
        MeetingInfoVo vo = requireMeeting(meetingId);
        List<SysUser> sysUsers = sysUserMapper.selectByIds(bo.getUserIds());

        Map<Long, MeetingParticipantsStatus> map = requireParticipantsMap(vo);
        List<Long> newUserIds = new ArrayList<>();

        for (SysUser sysUser : sysUsers) {
            if (map.containsKey(sysUser.getUserId())) {
                continue;
            }
            map.put(sysUser.getUserId(), buildParticipantStatus(sysUser.getUserId(), sysUser.getNickName(), 0));
            newUserIds.add(sysUser.getUserId());
        }

        MeetingRedisCache.addMeetingInfo(meetingId, vo);
        if (newUserIds.isEmpty()) {
            return;
        }
        String content = buildInviteContent(vo.getMeetingType(), vo.getHandleId(), vo.getInitiatorName(), vo.getPlanCode());
        sendMeetingJoinNotices(newUserIds, vo, content);
    }

    private MeetingInfoVo buildMeetingInfo(StartMeetingBo bo, Long meetingId, Long handleId, String initiatorName, List<Long> userIds, String planCode) {
        MeetingInfoVo vo = new MeetingInfoVo();
        vo.setMeetingId(meetingId);
        vo.setInitiator(bo.getInitiator());
        vo.setInitiatorName(initiatorName);
        vo.setHandleId(handleId);
        vo.setHandleProcess(bo.getHandleProcess());
        vo.setParticipantsMap(buildParticipantsMap(bo.getInitiator(), initiatorName, userIds));
        vo.setMeetingType(bo.getMeetingType());
        vo.setPlanCode(planCode);
        return vo;
    }

    private List<Long> normalizeUserIds(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return List.of();
        }
        return userIds.stream()
                      .filter(Objects::nonNull)
                      .distinct()
                      .toList();
    }

    private Map<Long, MeetingParticipantsStatus> buildParticipantsMap(Long initiator, String initiatorName, List<Long> userIds) {
        Map<Long, MeetingParticipantsStatus> participantsMap = new HashMap<>();
        participantsMap.put(initiator, buildParticipantStatus(initiator, initiatorName, 0));
        if (CollUtil.isEmpty(userIds)) {
            return participantsMap;
        }

        List<SysUser> sysUsers = sysUserMapper.selectByIds(userIds);
        for (SysUser sysUser : sysUsers) {
            participantsMap.put(sysUser.getUserId(), buildParticipantStatus(sysUser.getUserId(), sysUser.getNickName(), 0));
        }
        return participantsMap;
    }

    private MeetingParticipantsStatus buildParticipantStatus(Long userId, String nickName, Integer statusValue) {
        MeetingParticipantsStatus status = new MeetingParticipantsStatus();
        status.setUserId(userId);
        status.setNickName(nickName);
        status.setStatus(statusValue);
        return status;
    }

    private String buildInviteContent(Integer meetingType, Long handleId, String initiatorName, String planCode) {
        Map<String, String> params = new HashMap<>();
        params.put("initiator", initiatorName);

        String content = "";
        if (meetingType == 1) {
            DzTaskHandle dzTaskHandle = dzTaskHandleMapper.selectById(handleId);
            content = """
                关于{location}{eventType}{eventLevel}{respStatus}，{initiator}邀请您以专家身份参与相关《应急调查报告（编号：{detailId}）》专家会商，请尽快连线接入，谢谢您的支持与配合~
                """;
            params.put("location", dzTaskHandle.getProvince() + dzTaskHandle.getCity() + dzTaskHandle.getCounty() + dzTaskHandle.getStreet() + dzTaskHandle.getVillage());
            params.put("eventType", Objects.requireNonNull(EventTypeEnum.getByCode(dzTaskHandle.getEventType())).getName());
            params.put("eventLevel", requireEventLevelName(dzTaskHandle.getEventLevel()));
            params.put("respStatus", requireResponseStatusDisplayName(dzTaskHandle.getRespStatus()));
            params.put("detailId", handleId.toString());
        } else if (meetingType == 2 || meetingType == 4) {
            DefRespPlan defRespPlan = dzDefRespPlanMapper.selectById(handleId);
            Integer level = 1;
            if (defRespPlan != null) {
                level = defRespPlan.getLevel();
            }
            content = """
                关于恩施市{respStatus}，{initiator}邀请您以专家身份参与相关《恩施市防御响应方案（编号：{detailId}）》专家会商，请尽快连线接入，谢谢您的支持与配合~
                """;
            params.put("respStatus", requirePlanLevelResponseName(level));
            params.put("detailId", resolveDefRespInvitePlanCode(defRespPlan, planCode));
        } else if (meetingType == 3) {
            DzRiskPredictionPush dzRiskPredictionPush = dzRiskPredictionPushMapper.selectById(handleId);
            content = "关于{title}, {initiator}发起预测模式会议，请及时处理。";
            params.put("title", dzRiskPredictionPush.getTitle());
        }
        return StrUtil.format(content, params);
    }

    /**
     * 写入站内会议通知并推送 SSE；短信通道是可选扩展点，缺失时不能阻断会议创建。
     */
    private void sendMeetingJoinNotices(List<Long> userIds, MeetingInfoVo meetingInfo, String content) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        Map<Long, String> phoneByUserId = loadPhoneByUserId(userIds);
        Date now = new Date();
        for (Long userId : userIds) {
            DzMsgNotice notice = new DzMsgNotice();
            notice.setTitle(MEETING_INVITE_TITLE);
            notice.setContent(content);
            notice.setType(MEETING_JOIN_TYPE);
            notice.setStatus(0);
            notice.setBizData(JsonUtils.toJsonString(meetingInfo));
            notice.setUserId(userId);
            notice.setCreateDate(now);
            notice.setUpdateDate(now);
            dzMsgNoticeMapper.insert(notice);
            sendMeetingJoinSse(userId, notice);

            String phone = phoneByUserId.get(userId);
            if (StrUtil.isNotBlank(phone)) {
                sendMeetingInviteSms(phone, meetingInfo, userId, content);
            }
        }
    }

    /**
     * 通过可选窄端口发送邀请短信；目标项目未迁入短信业务时明确记录不可用，不伪造发送成功。
     */
    private void sendMeetingInviteSms(String phone, MeetingInfoVo meetingInfo, Long userId, String content) {
        if (meetingSmsSender.isEmpty()) {
            log.warn("会议邀请短信未发送，短信通道未配置, meetingId={}, userId={}", meetingInfo.getMeetingId(), userId);
            return;
        }
        try {
            boolean sent = meetingSmsSender.get().send(
                content,
                phone,
                resolveMeetingSmsBizType(meetingInfo.getMeetingType()),
                meetingInfo.getHandleId(),
                resolveReceiverName(meetingInfo, userId)
            );
            if (!sent) {
                log.warn("会议邀请短信未发送，短信通道返回失败, meetingId={}, userId={}", meetingInfo.getMeetingId(), userId);
            }
        } catch (Exception e) {
            log.warn("会议邀请短信发送失败, meetingId={}, userId={}", meetingInfo.getMeetingId(), userId, e);
        }
    }

    /**
     * 将会议类型映射为短信审计所需的业务类型，保持原模块的业务编码。
     */
    private Integer resolveMeetingSmsBizType(Integer meetingType) {
        if (Objects.equals(meetingType, 2) || Objects.equals(meetingType, 4)) {
            return MeetingSmsSender.BIZ_TYPE_DEF_RESP;
        }
        return MeetingSmsSender.BIZ_TYPE_TASK_PUSH;
    }

    private String resolveReceiverName(MeetingInfoVo meetingInfo, Long userId) {
        Map<Long, MeetingParticipantsStatus> participantsMap = meetingInfo.getParticipantsMap();
        if (participantsMap == null) {
            return null;
        }
        MeetingParticipantsStatus status = participantsMap.get(userId);
        return status == null ? null : status.getNickName();
    }

    private Map<Long, String> loadPhoneByUserId(List<Long> userIds) {
        List<SysUser> sysUsers = sysUserMapper.selectByIds(userIds);
        Map<Long, String> phoneByUserId = new HashMap<>();
        for (SysUser sysUser : sysUsers) {
            if (sysUser == null || sysUser.getUserId() == null || StrUtil.isBlank(sysUser.getPhonenumber())) {
                continue;
            }
            phoneByUserId.put(sysUser.getUserId(), sysUser.getPhonenumber().trim());
        }
        return phoneByUserId;
    }

    private void sendMeetingJoinSse(List<Long> userIds, Object data) {
        for (Long userId : userIds) {
            sendMeetingJoinSse(userId, data);
        }
    }

    private void sendMeetingJoinSse(Long userId, Object data) {
        SseResp.SseRespBuilder message = SseResp.builder()
                                                .type(MEETING_JOIN_TYPE)
                                                .data(data);
        sseEmitterManager.sendMessage(userId, message);
    }

    private MeetingInfoVo requireMeeting(Long meetingId) {
        return requireMeeting(meetingId, "未发现当前会议");
    }

    private MeetingInfoVo requireMeeting(Long meetingId, String message) {
        MeetingInfoVo meetingInfo = MeetingRedisCache.getMeetingInfo(meetingId);
        if (meetingInfo == null) {
            throw new ServiceException(message);
        }
        return meetingInfo;
    }

    private Map<Long, MeetingParticipantsStatus> requireParticipantsMap(MeetingInfoVo meetingInfo) {
        Map<Long, MeetingParticipantsStatus> participantsMap = meetingInfo.getParticipantsMap();
        if (participantsMap == null) {
            throw new ServiceException("会议参与人信息不存在");
        }
        return participantsMap;
    }

    private void requireInitiator(MeetingInfoVo meetingInfo, String message) {
        if (isInitiator(meetingInfo, LoginHelper.getUserId())) {
            throw new ServiceException(message);
        }
    }

    private boolean isInitiator(MeetingInfoVo meetingInfo, Long userId) {
        return !Objects.equals(userId, meetingInfo.getInitiator());
    }

    private void updateParticipantStatus(MeetingInfoVo meetingInfo, Long userId, Integer statusValue) {
        MeetingParticipantsStatus status = requireParticipantsMap(meetingInfo).get(userId);
        if (status == null) {
            throw new ServiceException("当前用户不在会议参与人列表中");
        }
        status.setStatus(statusValue);
    }

    private MeetingJoinResp buildJoinResp(MeetingInfoVo meetingInfo, Long meetingId) {
        LiveKitTokenReq req = new LiveKitTokenReq();
        req.setMeetingId(meetingId);

        MeetingJoinResp resp = new MeetingJoinResp();
        resp.setMeetingInfo(meetingInfo);
        resp.setLiveKit(liveKitService.generateToken(req));
        return resp;
    }

    private String requireEventLevelName(Integer eventLevelCode) {
        String levelName = LevelCodeUtil.resolveEventLevelName(eventLevelCode);
        if (StrUtil.isBlank(levelName)) {
            throw new ServiceException("险情规模不存在");
        }
        return levelName;
    }

    private String requireResponseStatusDisplayName(Integer responseStatusCode) {
        String displayName = ResponseStatusEnum.getDisplayName(responseStatusCode);
        if (StrUtil.isBlank(displayName)) {
            throw new ServiceException("响应状态不存在");
        }
        return displayName;
    }

    private String requirePlanLevelResponseName(Integer level) {
        String displayName = LevelCodeUtil.resolveDefenseResponseDisplayName(level);
        if (StrUtil.isBlank(displayName)) {
            throw new ServiceException("防御响应级别不存在");
        }
        return displayName;
    }

    private String requirePlanCode(DefRespPlan defRespPlan) {
        if (defRespPlan == null || StrUtil.isBlank(defRespPlan.getCode())) {
            throw new ServiceException("防御响应方案编号不存在");
        }
        return defRespPlan.getCode();
    }

    private String resolveDefRespInvitePlanCode(DefRespPlan defRespPlan, String planCode) {
        if (StrUtil.isNotBlank(planCode)) {
            return planCode;
        }
        return requirePlanCode(defRespPlan);
    }
}
