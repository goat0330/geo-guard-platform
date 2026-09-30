/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import cn.edu.pku.whai.geological.disaster.data.utils.LevelCodeUtil;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DzTaskHandleApprovalTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.EventLevelEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.EventTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.SysRoleEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleApprovalBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzMsgNotice;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSmsSendBatch;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleApprovalVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserContactVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzMsgNoticeMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.meeting.cache.MeetingRedisCache;
import cn.edu.pku.whai.geological.disaster.service.meeting.domain.vo.MeetingInfoVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleApprovalService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.AdminApprovalNoticeContext;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsScene;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendContext;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendService;
import cn.edu.pku.whai.geological.disaster.service.sse.SseEmitterManager;
import cn.edu.pku.whai.geological.disaster.service.sse.domain.resp.SseResp;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysUserService;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
class DzDefRespAdminApprovalDelegate {

    private static final int DEF_RESP_MEETING_TYPE = DzDefRespPlanServiceImpl.DEF_RESP_MEETING_TYPE;
    private static final int DEF_RESP_MEETING_TYPE_A = DzDefRespPlanServiceImpl.DEF_RESP_MEETING_TYPE_A;
    private static final String ADMIN_APPROVAL_SMS_TITLE = DzDefRespPlanServiceImpl.ADMIN_APPROVAL_SMS_TITLE;

    private final DzDefRespPlanServiceImpl service;
    private final IDzTaskHandleApprovalService dzTaskHandleApprovalService;
    private final TransactionTemplate transactionTemplate;
    private final ISysUserService sysUserService;
    private final DzMsgNoticeMapper dzMsgNoticeMapper;
    private final SseEmitterManager sseEmitterManager;
    private final SmsSendService smsSendService;
    private final DzTaskHandleMapper dzTaskHandleMapper;
    private final IDzUserAdRegionService dzUserAdRegionService;

    DzDefRespAdminApprovalDelegate(DzDefRespPlanServiceImpl service,
                                   IDzTaskHandleApprovalService dzTaskHandleApprovalService,
                                   TransactionTemplate transactionTemplate,
                                   ISysUserService sysUserService,
                                   DzMsgNoticeMapper dzMsgNoticeMapper,
                                   SseEmitterManager sseEmitterManager,
                                   SmsSendService smsSendService,
                                   DzTaskHandleMapper dzTaskHandleMapper,
                                   IDzUserAdRegionService dzUserAdRegionService) {
        this.service = service;
        this.dzTaskHandleApprovalService = dzTaskHandleApprovalService;
        this.transactionTemplate = transactionTemplate;
        this.sysUserService = sysUserService;
        this.dzMsgNoticeMapper = dzMsgNoticeMapper;
        this.sseEmitterManager = sseEmitterManager;
        this.smsSendService = smsSendService;
        this.dzTaskHandleMapper = dzTaskHandleMapper;
        this.dzUserAdRegionService = dzUserAdRegionService;
    }

    private DefRespPlan getDefRespPlanById(Long defId) { return service.getDefRespPlanById(defId); }
    boolean hasPendingAdminApproval(Long handleId, Integer meetingType, Integer roundNo) {
        DzTaskHandleApprovalBo queryBo = new DzTaskHandleApprovalBo();
        queryBo.setHandleId(handleId);
        queryBo.setMeetingType(meetingType);
        queryBo.setType(DzTaskHandleApprovalTypeEnum.ADMIN_APPROVAL.getCode());
        queryBo.setRoundNo(roundNo);
        queryBo.setStatus(0);
        List<DzTaskHandleApprovalVo> approvals = dzTaskHandleApprovalService.queryList(queryBo);
        return approvals != null && !approvals.isEmpty();
    }

    /**
     * 发送审批通知并新增行政审批记录
     */
    public R<String> insertExecutiveByBo(Long handleId, Integer meetingType, Integer handleProcess, Integer roundNo) {
        return transactionTemplate.execute(status -> {
            DzTaskHandleApprovalBo dzTaskHandleApprovalBo = new DzTaskHandleApprovalBo();
            dzTaskHandleApprovalBo.setHandleId(handleId);
            dzTaskHandleApprovalBo.setMeetingType(meetingType);
            dzTaskHandleApprovalBo.setType(DzTaskHandleApprovalTypeEnum.ADMIN_APPROVAL.getCode());
            dzTaskHandleApprovalBo.setProcess(handleProcess);
            dzTaskHandleApprovalBo.setStatus(0);
            int targetRoundNo = roundNo == null ? 1 : roundNo;
            if (isDefRespMeetingType(meetingType)) {
                DefRespPlan defRespPlan = getDefRespPlanById(handleId);
                targetRoundNo = defRespPlan.getCurrentRoundNo() == null ? 1 : defRespPlan.getCurrentRoundNo();
            }
            dzTaskHandleApprovalBo.setRoundNo(targetRoundNo);
            if (hasPendingAdminApproval(handleId, meetingType, targetRoundNo)) {
                log.info("行政审批通知已存在，跳过重复发送 handleId={}, meetingType={}, roundNo={}",
                    handleId, meetingType, targetRoundNo);
                return R.ok("审批通知已发送，跳过重复发送");
            }
            Date now = new Date();
            AdminApprovalNoticeContext noticeContext = buildAdminApprovalNoticeContext(handleId, meetingType, handleProcess);
            if (noticeContext.userContact() == null || StringUtils.isBlank(noticeContext.userContact().getPhoneNumber())) {
                throw new ServiceException("未查到该角色电话信息");
            }
            String phone = noticeContext.userContact().getPhoneNumber();
            SysUserVo sysUserVo = sysUserService.selectUserByPhonenumber(phone);
            if (sysUserVo == null) {
                throw new ServiceException("用户不存在");
            }
            Long userId = sysUserVo.getUserId();
            dzTaskHandleApprovalBo.setUserId(userId);
            String content = StrUtil.format(noticeContext.contentTemplate(), noticeContext.params());
            DzMsgNotice notice = new DzMsgNotice();
            notice.setTitle(ADMIN_APPROVAL_SMS_TITLE);
            notice.setContent(content);
            notice.setType("approval-msg");
            notice.setStatus(0);
            notice.setBizData(JsonUtils.toJsonString(resolveMeetingInfo(noticeContext.meetingInfo(), handleId, meetingType)));
            notice.setUserId(userId);
            notice.setCreateDate(now);
            notice.setUpdateDate(now);
            dzMsgNoticeMapper.insert(notice);
            dzTaskHandleApprovalBo.setNickname(sysUserVo.getNickName());

            if (!dzTaskHandleApprovalService.insertByBo(dzTaskHandleApprovalBo)) {
                throw new ServiceException("新增行政审批记录失败");
            }

            SseResp.SseRespBuilder message = SseResp.builder()
                                                    .type("approval-msg")
                                                    .data(notice);
            sseEmitterManager.sendMessage(userId, message);
            sendAdminApprovalSms(handleId, meetingType, sysUserVo.getNickName(), phone, content);

            return R.ok("已向id为" + userId + "的用户发送消息");
        });
    }

    AdminApprovalNoticeContext buildAdminApprovalNoticeContext(Long handleId, Integer meetingType, Integer handleProcess) {
        if (Objects.equals(meetingType, 1)) {
            return buildTaskAdminApprovalNoticeContext(handleId, meetingType, handleProcess);
        }
        if (isDefRespMeetingType(meetingType)) {
            return buildDefRespAdminApprovalNoticeContext(handleId);
        }
        if (Objects.equals(meetingType, 3)) {
            throw new ServiceException("预测模式功能开发中");
        }
        return new AdminApprovalNoticeContext(null, null, "", new HashMap<>());
    }

    AdminApprovalNoticeContext buildTaskAdminApprovalNoticeContext(Long handleId, Integer meetingType, Integer handleProcess) {
        Long meetingId = MeetingRedisCache.getHandleProcess(handleId, handleProcess);
        MeetingInfoVo meetingInfo = MeetingRedisCache.getMeetingInfo(meetingId);
        if (meetingInfo == null) {
            throw new ServiceException("未找到对应的会议信息");
        }
        meetingInfo.setMeetingType(meetingType);
        meetingInfo.setHandleId(handleId);
        meetingInfo.setApprovalType(DzTaskHandleApprovalTypeEnum.ADMIN_APPROVAL.getCode());
        DzTaskHandle dzTaskHandle = dzTaskHandleMapper.selectById(handleId);
        if (dzTaskHandle == null) {
            throw new ServiceException("处置记录不存在");
        }
        DzUserContactVo userContact = resolveFirstUserContact(
            dzTaskHandle.getCounty(), dzTaskHandle.getStreet(), null, 4, SysRoleEnum.DZ_FGXIANGZHANG.getRoleKey());
        Map<String, String> params = new HashMap<>();
        params.put("location", dzTaskHandle.getProvince() + dzTaskHandle.getCity() + dzTaskHandle.getCounty()
            + dzTaskHandle.getStreet() + dzTaskHandle.getVillage());
        params.put("eventType", requireEventTypeName(dzTaskHandle.getEventType()));
        params.put("eventLevel", requireEventLevelName(dzTaskHandle.getEventLevel()));
        params.put("respStatus", requireResponseStatusLevelName(dzTaskHandle.getRespStatus()));
        params.put("initiator", requireUserNickName(meetingInfo.getInitiator()));
        params.put("handleId", handleId.toString());
        String content = """
            由{initiator}提交关于{location}{eventLevel}{eventType}的《{respStatus}防御响应方案（编号：{handleId}）》已流转至您的审批环节，灾情刻不容缓，请尽快处理，谢谢您的支持与配合~
            """;
        return new AdminApprovalNoticeContext(meetingInfo, userContact, content, params);
    }

    AdminApprovalNoticeContext buildDefRespAdminApprovalNoticeContext(Long handleId) {
        DefRespPlan defRespPlan = getDefRespPlanById(handleId);
        DzUserContactVo userContact = resolveFirstUserContact(
            defRespPlan.getCounty(), null, null, 3, SysRoleEnum.DZ_FGXIANZHANG.getRoleKey());
        DzTaskHandle dzTaskHandle = null;
        if (DefRespPlanTypeEnum.SINGLE.getCode().equals(defRespPlan.getType()) && defRespPlan.getHandleId() != null) {
            dzTaskHandle = dzTaskHandleMapper.selectById(defRespPlan.getHandleId());
        }
        Map<String, String> params = new HashMap<>();
        params.put("location", buildApprovalLocation(defRespPlan, dzTaskHandle));
        params.put("eventType", dzTaskHandle != null ? requireEventTypeName(dzTaskHandle.getEventType()) : "");
        params.put("eventLevel", dzTaskHandle != null ? requireEventLevelName(dzTaskHandle.getEventLevel()) : "");
        params.put("planLevel", requirePlanLevelName(defRespPlan.getLevel()));
        params.put("planCode", requirePlanCode(defRespPlan));
        params.put("initiator", defRespPlan.getResponsiblePerson());
        String content = """
            由{initiator}提交关于{location}{eventLevel}{eventType}的《{planLevel}防御响应方案（编号：{planCode}）》已流转至您的审批环节，灾情刻不容缓，请尽快处理，谢谢您的支持与配合~
            """;
        return new AdminApprovalNoticeContext(null, userContact, content, params);
    }

    private DzUserContactVo resolveFirstUserContact(String county, String street, String village, Integer level, String roleKey) {
        List<DzUserContactVo> userContacts = dzUserAdRegionService.resolveUsers(county, street, village, level, roleKey);
        if (userContacts == null || userContacts.isEmpty()) {
            throw new ServiceException("未查到用户姓名和手机号");
        }
        return userContacts.stream()
                           .filter(Objects::nonNull)
                           .filter(item -> StringUtils.isNotBlank(item.getPhoneNumber()))
                           .findFirst()
                           .orElseThrow(() -> new ServiceException("未查到用户姓名和手机号"));
    }

    MeetingInfoVo resolveMeetingInfo(MeetingInfoVo meetingInfo, Long handleId, Integer meetingType) {
        if (meetingInfo != null) {
            return meetingInfo;
        }
        MeetingInfoVo result = new MeetingInfoVo();
        result.setMeetingType(meetingType);
        result.setHandleId(handleId);
        result.setApprovalType(DzTaskHandleApprovalTypeEnum.ADMIN_APPROVAL.getCode());
        return result;
    }

    boolean isDefRespMeetingType(Integer meetingType) {
        return Objects.equals(meetingType, DEF_RESP_MEETING_TYPE) || Objects.equals(meetingType, DEF_RESP_MEETING_TYPE_A);
    }

    void sendAdminApprovalSms(Long handleId, Integer meetingType, String receiverName, String phone, String content) {
        if (StringUtils.isBlank(phone)) {
            return;
        }
        try {
            smsSendService.send(content, phone, SmsSendContext.builder()
                                                              .scene(SmsScene.ADMIN_APPROVAL)
                                                              .bizType(resolveAdminApprovalSmsBizType(meetingType))
                                                              .bizId(handleId)
                                                              .smsType(ADMIN_APPROVAL_SMS_TITLE)
                                                              .receiverName(receiverName)
                                                              .build());
        } catch (Exception e) {
            log.warn("行政审批短信发送失败, phone={}, handleId={}", phone, handleId, e);
        }
    }

    Integer resolveAdminApprovalSmsBizType(Integer meetingType) {
        if (isDefRespMeetingType(meetingType)) {
            return DzSmsSendBatch.BIZ_TYPE_DEF_RESP;
        }
        return DzSmsSendBatch.BIZ_TYPE_TASK_PUSH;
    }

    /**
     * 获取灾害类型名称
     */
    String requireEventTypeName(Integer eventTypeCode) {
        EventTypeEnum eventTypeEnum = EventTypeEnum.getByCode(eventTypeCode);
        if (eventTypeEnum == null || StringUtils.isBlank(eventTypeEnum.getName())) {
            throw new ServiceException("灾害类型不存在");
        }
        return eventTypeEnum.getName();
    }

    /**
     * 获取响应状态等级名称
     */
    String requireResponseStatusLevelName(Integer responseStatusCode) {
        String result = LevelCodeUtil.resolveDefenseResponseRomanLevel(responseStatusCode);
        if (StringUtils.isBlank(result)) {
            throw new ServiceException("响应状态不存在");
        }
        return result;
    }

    /**
     * 获取险情规模名称
     */
    String requireEventLevelName(Integer eventLevelCode) {
        EventLevelEnum eventLevelEnum = EventLevelEnum.getByCode(eventLevelCode);
        if (eventLevelEnum == null || StringUtils.isBlank(eventLevelEnum.getName())) {
            throw new ServiceException("险情规模不存在");
        }
        return eventLevelEnum.getName();
    }

    /**
     * 获取方案响应级别名称
     */
    String requirePlanLevelName(Integer level) {
        String result = LevelCodeUtil.resolveDefenseResponseRomanLevel(level);
        if (StringUtils.isBlank(result)) {
            throw new ServiceException("防御响应级别不存在");
        }
        return result;
    }

    /**
     * 获取方案编号
     */
    String requirePlanCode(DefRespPlan defRespPlan) {
        if (defRespPlan == null || StringUtils.isBlank(defRespPlan.getCode())) {
            throw new ServiceException("防御响应方案编号不存在");
        }
        return defRespPlan.getCode();
    }

    /**
     * 构建审批通知地点文案
     */
    String buildApprovalLocation(DefRespPlan defRespPlan, DzTaskHandle dzTaskHandle) {
        if (dzTaskHandle != null) {
            return dzTaskHandle.getProvince() + dzTaskHandle.getCity() + dzTaskHandle.getCounty()
                + dzTaskHandle.getStreet() + dzTaskHandle.getVillage();
        }
        return StringUtils.defaultString(defRespPlan.getCounty()) + StringUtils.defaultString(defRespPlan.getStreets());
    }

    /**
     * 获取用户昵称
     */
    String requireUserNickName(Long userId) {
        SysUserVo initiator = sysUserService.selectUserById(userId);
        if (initiator == null || StringUtils.isBlank(initiator.getNickName())) {
            throw new ServiceException("会议发起人不存在");
        }
        return initiator.getNickName();
    }
}
