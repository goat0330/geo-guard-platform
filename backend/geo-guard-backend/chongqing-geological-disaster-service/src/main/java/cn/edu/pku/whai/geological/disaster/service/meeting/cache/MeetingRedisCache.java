/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.cache;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.redis.utils.RedisUtils;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.vo.MeetingInfoVo;
import cn.hutool.core.util.ObjUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MeetingRedisCache {

    private static final String MEETING_INFO_KEY = "meeting:info:";
    private static final String HANDLE_KEY = "meeting:handle:process:";
    private static final long DEF_RESP_PLACEHOLDER_HANDLE_ID = 10000000L;
    private static final int DEF_RESP_PLACEHOLDER_PROCESS = 1;

    private static String getInfoKey(Long meetingId) {
        return MEETING_INFO_KEY + meetingId;
    }

    private static String getHandleKey(Long meetingId, Integer process) {
        return HANDLE_KEY + meetingId + ":" + process;
    }

    public static void closeMeeting(Long meetingId) {
        MeetingInfoVo meetingInfo = getMeetingInfo(meetingId);
        if (meetingInfo == null) {
            throw new ServiceException("会议结束或者会议不存在");
        }

        Long initiator = meetingInfo.getInitiator();
//        if (!ObjUtil.equals(LoginHelper.getUserId(), initiator)) {
//            throw new ServiceException("只有主持人才能关闭会议");
//        }

        Long placeholderMeetingId = getHandleProcess(DEF_RESP_PLACEHOLDER_HANDLE_ID, DEF_RESP_PLACEHOLDER_PROCESS);
        if (ObjUtil.equal(placeholderMeetingId, meetingId)) {
            RedisUtils.deleteObject(getHandleKey(DEF_RESP_PLACEHOLDER_HANDLE_ID, DEF_RESP_PLACEHOLDER_PROCESS));
        } else {
            RedisUtils.deleteObject(getHandleKey(meetingInfo.getHandleId(), meetingInfo.getHandleProcess()));
        }
        RedisUtils.deleteObject(getInfoKey(meetingId));
    }

    public static MeetingInfoVo getMeetingInfo(Long meetingId) {
        return RedisUtils.getCacheObject(getInfoKey(meetingId));
    }

    public static void addMeetingInfo(Long meetingId, MeetingInfoVo meetingInfo) {
        RedisUtils.setCacheObject(getInfoKey(meetingId), meetingInfo);
    }

    public static void addHandleProcess(Long handleId, Integer process, Long meetingId) {
        RedisUtils.setCacheObject(getHandleKey(handleId, process), meetingId);
    }

    public static Long getHandleProcess(Long handleId, Integer process) {
        return convertToLong(RedisUtils.getCacheObject(getHandleKey(handleId, process)));
    }

    private static Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        if (value instanceof String stringValue) {
            return Long.parseLong(stringValue);
        }
        throw new ServiceException("会议缓存数据格式错误");
    }

    public static List<Long> listMeetingIdsByHandleId(Long handleId) {
        if (handleId == null) {
            return List.of();
        }
        Collection<String> keys = RedisUtils.keys(MEETING_INFO_KEY + "*");
        List<Long> meetingIds = new ArrayList<>();
        for (String key : keys) {
            MeetingInfoVo meetingInfo = RedisUtils.getCacheObject(key);
            if (meetingInfo == null || !Objects.equals(meetingInfo.getHandleId(), handleId)) {
                continue;
            }
            if (meetingInfo.getMeetingId() != null) {
                meetingIds.add(meetingInfo.getMeetingId());
            }
        }
        return meetingIds;
    }

    public static List<Long> listMeetingIdsByHandleProcessKey(Long handleId) {
        if (handleId == null) {
            return List.of();
        }
        Collection<String> keys = RedisUtils.keys(HANDLE_KEY + handleId + ":*");
        List<Long> meetingIds = new ArrayList<>();
        for (String key : keys) {
            Long meetingId = convertToLong(RedisUtils.getCacheObject(key));
            if (meetingId != null) {
                meetingIds.add(meetingId);
            }
        }
        return meetingIds;
    }

    public static Long countMeetingsByParticipant(Long userId) {
        if (userId == null) {
            return 0L;
        }
        Collection<String> keys = RedisUtils.keys(MEETING_INFO_KEY + "*");
        long count = 0L;
        for (String key : keys) {
            MeetingInfoVo meetingInfo = RedisUtils.getCacheObject(key);
            if (meetingInfo == null) {
                continue;
            }
            Map<Long, ?> participantsMap = meetingInfo.getParticipantsMap();
            if (participantsMap != null && participantsMap.containsKey(userId)) {
                count++;
            }
        }
        return count;
    }

}
