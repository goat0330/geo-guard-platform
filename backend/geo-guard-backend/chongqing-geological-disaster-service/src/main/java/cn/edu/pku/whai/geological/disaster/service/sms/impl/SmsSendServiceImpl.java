/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sms.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzSmsSendBatchBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzSmsSendStatBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSmsSendBatch;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.EvacuationSmsResult;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.SmsTestSendVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzSmsSendBatchService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzSmsSendStatService;
import cn.edu.pku.whai.geological.disaster.service.service.ISmsConfigService;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendContext;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsScene;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendService;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsSendService.BatchSendItem;
import cn.edu.pku.whai.geological.disaster.service.sms.config.SmsConfigSnapshot;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.kongweiguang.http.client.Req;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 短信发送能力实现；传入 {@link SmsSendContext} 时自动写入发送记录。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SmsSendServiceImpl implements SmsSendService {

    private static final String DEFAULT_SMS_ERROR_MSG = "短信发送失败";
    private static final String DEFAULT_BATCH_TARGET_TYPE = "PERSON";
    private static final String DUPLICATE_BLOCK_ERROR_TEMPLATE = "本地服务器拦截:%d分钟内禁止重复发送";
    private static final String PLATFORM_SUCCESS_CODE = "00";
    private static final String PLATFORM_MESSAGE_FIELD = "message";
    private static final Duration SMS_REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final IDzSmsSendStatService dzSmsSendStatService;
    private final IDzSmsSendBatchService dzSmsSendBatchService;
    private final ISmsConfigService smsConfigService;

    @Override
    public EvacuationSmsResult send(String content, List<String> phones) {
        return send(content, phones, null);
    }

    @Override
    public EvacuationSmsResult send(String content, List<String> phones, SmsSendContext context) {
        SmsConfigSnapshot snapshot = smsConfigService.getSnapshot();
        if (phones == null || phones.isEmpty()) {
            return EvacuationSmsResult.builder()
                                      .successCount(0)
                                      .failCount(0)
                                      .skipCount(0)
                                      .failedPhones(Collections.emptyList())
                                      .build();
        }
        prepareBatchContext(context);

        String sendContent = buildSignedContent(content, snapshot);
        List<PhoneSendResult> sendResults = new ArrayList<>();
        List<String> pendingPhones = new ArrayList<>();
        for (String phone : phones) {
            String normalizedPhone = normalizePhone(phone);
            SingleSendResult preResult = resolvePreSendResult(sendContent, normalizedPhone, true, context, snapshot);
            if (preResult == null) {
                pendingPhones.add(normalizedPhone);
            } else {
                sendResults.add(new PhoneSendResult(normalizedPhone, sendContent, preResult));
            }
        }

        if (!pendingPhones.isEmpty()) {
            SingleSendResult platformResult = doSend(sendContent, pendingPhones, snapshot);
            for (String phone : pendingPhones) {
                sendResults.add(new PhoneSendResult(phone, sendContent, platformResult));
            }
        }

        EvacuationSmsResult result = buildResultAndRecord(sendContent, sendResults, context);
        log.debug("短信发送接口：成功 {} 条，失败 {} 条，正文长度 {}",
            result.getSuccessCount(), result.getFailCount(), sendContent != null ? sendContent.length() : 0);
        return result;
    }

    @Override
    public EvacuationSmsResult batchSend(List<BatchSendItem> items, SmsSendContext context) {
        SmsConfigSnapshot snapshot = smsConfigService.getSnapshot();
        if (items == null || items.isEmpty()) {
            return EvacuationSmsResult.builder()
                                      .successCount(0)
                                      .failCount(0)
                                      .skipCount(0)
                                      .failedPhones(Collections.emptyList())
                                      .build();
        }
        prepareBatchContext(context);

        List<PhoneSendResult> sendResults = new ArrayList<>();
        List<BatchSendPayloadItem> pendingItems = new ArrayList<>();
        for (BatchSendItem item : items) {
            String mobile = item == null ? null : normalizePhone(item.mobile());
            String sendContent = buildSignedContent(item == null ? null : item.content(), snapshot);
            SingleSendResult preResult = resolvePreSendResult(sendContent, mobile, true, context, snapshot);
            if (preResult == null) {
                pendingItems.add(new BatchSendPayloadItem(mobile, sendContent));
            } else {
                sendResults.add(new PhoneSendResult(mobile, sendContent, preResult));
            }
        }

        if (!pendingItems.isEmpty()) {
            SingleSendResult platformResult = doBatchSend(pendingItems, snapshot);
            for (BatchSendPayloadItem item : pendingItems) {
                sendResults.add(new PhoneSendResult(item.mobile(), item.content(), platformResult));
            }
        }

        String batchContentSnapshot = buildBatchContentSnapshot(sendResults);
        EvacuationSmsResult result = buildResultAndRecord(batchContentSnapshot, sendResults, context);
        log.debug("点对点批量短信发送接口：成功 {} 条，失败 {} 条，总数 {}",
            result.getSuccessCount(), result.getFailCount(), result.getTotalCount());
        return result;
    }

    @Override
    public Boolean send(String content, String phone) {
        return send(content, phone, null);
    }

    @Override
    public Boolean send(String content, String phone, SmsSendContext context) {
        SmsConfigSnapshot snapshot = smsConfigService.getSnapshot();
        String normalizedPhone = normalizePhone(phone);
        String sendContent = buildSignedContent(content, snapshot);
        SingleSendResult result = resolvePreSendResult(sendContent, normalizedPhone, false, context, snapshot);
        if (result == null) {
            result = doSend(sendContent, normalizedPhone, snapshot);
        }
        if (isAuditContext(context)) {
            recordSmsSendStat(normalizedPhone, sendContent, result, context);
        }
        return result.success();
    }

    @Override
    public SmsTestSendVo testSend(String content, String phone) {
        SmsConfigSnapshot snapshot = smsConfigService.getSnapshot();
        String normalizedPhone = normalizePhone(phone);
        String sendContent = buildSignedContent(content, snapshot);

        SmsTestSendVo vo = new SmsTestSendVo();
        vo.setPhone(normalizedPhone);
        vo.setContent(sendContent);
        vo.setSmsEnabled(snapshot.isPlatformEnabled() && snapshot.isTestEnabled());

        if (StringUtils.isBlank(normalizedPhone)) {
            vo.setSuccess(false);
            vo.setMessage("手机号不能为空");
            return vo;
        }

        if (!snapshot.isPlatformEnabled()) {
            vo.setSuccess(false);
            vo.setMessage("短信平台总开关已关闭，未发送测试短信");
            return vo;
        }
        if (!snapshot.isTestEnabled()) {
            vo.setSuccess(false);
            vo.setMessage("测试短信开关已关闭，未发送测试短信");
            return vo;
        }

        SingleSendResult result = doSend(sendContent, normalizedPhone, snapshot);
        vo.setSuccess(result.success());
        vo.setMessage(resolveDisplayMessage(result));
        return vo;
    }

    @Override
    public JsonNode queryReport() {
        SmsConfigSnapshot snapshot = smsConfigService.getSnapshot();
        validatePlatformConfigOrThrow(snapshot);
        try {
            return Req.post(normalizeBaseUrl(snapshot))
                      .path(normalizePath(snapshot.getReportPath()))
                      .header("Content-Type", "application/json;charset=UTF-8")
                      .json(buildBasePlatformBody(snapshot))
                      .timeout(SMS_REQUEST_TIMEOUT)
                      .ok()
                      .node();
        } catch (Exception e) {
            log.error("查询短信状态报告失败", e);
            throw new ServiceException("查询短信状态报告失败：" + e.getMessage());
        }
    }

    private SingleSendResult resolvePreSendResult(String content, String phone, boolean batchSend,
                                                  SmsSendContext context, SmsConfigSnapshot snapshot) {
        if (StringUtils.isBlank(phone)) {
            return SingleSendResult.fail("手机号为空");
        }
        String disabledReason = resolveDisabledReason(context, snapshot);
        if (disabledReason != null) {
            log.warn("短信发送被动态配置跳过，场景：{}，任务来源：{}，手机号：{}，原因：{}",
                context == null ? null : context.getScene(),
                context == null ? null : context.getTaskSourceType(), phone, disabledReason);
            return SingleSendResult.skipped(disabledReason);
        }
        if (isRecentlyDuplicate(phone, content, batchSend, snapshot.getDedupMinutes())) {
            log.warn("短信发送被本地去重拦截，手机号：{}，正文长度：{}", phone, content != null ? content.length() : 0);
            return SingleSendResult.fail(buildDuplicateBlockErrorMsg(snapshot.getDedupMinutes()));
        }
        return null;
    }

    private String resolveDisabledReason(SmsSendContext context, SmsConfigSnapshot snapshot) {
        if (!snapshot.isPlatformEnabled()) {
            return "短信平台总开关已关闭";
        }
        if (context == null || context.getScene() == null) {
            return "短信发送场景未指定";
        }
        SmsScene scene = context.getScene();
        if (scene == SmsScene.CAPTCHA) {
            return snapshot.isCaptchaEnabled() ? null : "短信验证码开关已关闭";
        }
        if (scene == SmsScene.TEST_SEND) {
            return snapshot.isTestEnabled() ? null : "测试短信开关已关闭";
        }
        if (!snapshot.isBusinessEnabled()) {
            return "业务短信总开关已关闭";
        }
        return switch (scene) {
            case TASK_PUSH -> resolveTaskPushDisabledReason(context, snapshot);
            case EVACUATION -> snapshot.isEvacuationEnabled() ? null : "群众撤离短信开关已关闭";
            case DEF_RESP_START -> snapshot.isDefRespStartEnabled() ? null : "防御响应启动短信开关已关闭";
            case MEETING_INVITE -> snapshot.isMeetingInviteEnabled() ? null : "会议邀请短信开关已关闭";
            case ADMIN_APPROVAL -> snapshot.isAdminApprovalEnabled() ? null : "行政审批短信开关已关闭";
            case CAPTCHA, TEST_SEND -> null;
        };
    }

    private String resolveTaskPushDisabledReason(SmsSendContext context, SmsConfigSnapshot snapshot) {
        if (!snapshot.isTaskPushEnabled()) {
            return "任务推送短信开关已关闭";
        }
        Integer sourceType = context.getTaskSourceType();
        if (sourceType == null || !snapshot.getTaskPushAllowedSourceTypes().contains(sourceType)) {
            return "任务来源未启用短信发送，sourceType=" + sourceType;
        }
        return null;
    }

    private boolean isRecentlyDuplicate(String phone, String content, boolean batchSend, int dedupMinutes) {
        if (dedupMinutes <= 0 || StringUtils.isBlank(phone) || StringUtils.isBlank(content)) {
            return false;
        }
        Date since = buildDedupSinceTime(dedupMinutes);
        String normalizedPhone = phone.trim();
        if (dzSmsSendStatService.existsRecentByPhoneAndContent(normalizedPhone, content, since)) {
            return true;
        }
        return batchSend && dzSmsSendBatchService.existsRecentByPhoneAndContent(normalizedPhone, content, since);
    }

    private Date buildDedupSinceTime(int dedupMinutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -dedupMinutes);
        return calendar.getTime();
    }

    private String buildDuplicateBlockErrorMsg(int dedupMinutes) {
        return String.format(DUPLICATE_BLOCK_ERROR_TEMPLATE, dedupMinutes);
    }

    private SingleSendResult doSend(String content, String phone, SmsConfigSnapshot snapshot) {
        return doSend(content, List.of(phone), snapshot);
    }

    private SingleSendResult doSend(String content, List<String> phones, SmsConfigSnapshot snapshot) {
        String configError = validatePlatformConfig(snapshot);
        if (configError != null) {
            return SingleSendResult.fail(configError);
        }
        List<String> validPhones = phones == null ? List.of() : phones.stream()
                                                                       .filter(StringUtils::isNotBlank)
                                                                       .map(String::trim)
                                                                       .toList();
        if (validPhones.isEmpty()) {
            return SingleSendResult.fail("手机号为空");
        }

        String mobiles = String.join(",", validPhones);
        Map<String, Object> requestBody = buildBasePlatformBody(snapshot);
        requestBody.put("taskId", buildTaskId());
        requestBody.put("mobiles", mobiles);
        requestBody.put("content", buildSignedContent(content, snapshot));
        requestBody.put("extNo", "");
        return postPlatformRequest(snapshot.getSendPath(), requestBody, "短信发送", mobiles, snapshot);
    }

    private SingleSendResult doBatchSend(List<BatchSendPayloadItem> items, SmsConfigSnapshot snapshot) {
        String configError = validatePlatformConfig(snapshot);
        if (configError != null) {
            return SingleSendResult.fail(configError);
        }
        List<BatchSendPayloadItem> validItems = items == null ? List.of() : items.stream()
                                                                                 .filter(item -> item != null && StringUtils.isNotBlank(item.mobile()))
                                                                                 .toList();
        if (validItems.isEmpty()) {
            return SingleSendResult.fail("手机号为空");
        }

        List<Map<String, String>> data = new ArrayList<>();
        for (BatchSendPayloadItem item : validItems) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("mobile", item.mobile());
            row.put("content", buildSignedContent(item.content(), snapshot));
            data.add(row);
        }

        Map<String, Object> requestBody = buildBasePlatformBody(snapshot);
        requestBody.put("taskId", buildTaskId());
        requestBody.put("data", data);
        String mobiles = validItems.stream().map(BatchSendPayloadItem::mobile).collect(Collectors.joining(","));
        return postPlatformRequest(snapshot.getBatchSendPath(), requestBody, "点对点批量短信发送", mobiles, snapshot);
    }

    private SingleSendResult postPlatformRequest(String path, Map<String, Object> requestBody, String action,
                                                 String target, SmsConfigSnapshot snapshot) {
        try {
            JsonNode root = Req.post(normalizeBaseUrl(snapshot))
                               .path(normalizePath(path))
                               .header("Content-Type", "application/json;charset=UTF-8")
                               .json(requestBody)
                               .timeout(SMS_REQUEST_TIMEOUT)
                               .ok()
                               .node();
            SingleSendResult result = parsePlatformResult(root);
            if (result.success()) {
                log.debug("{}成功，目标：{}，平台消息：{}", action, target, result.platformMessage());
            } else {
                log.error("{}失败，目标：{}，错误：{}", action, target, result.errorMsg());
            }
            return result;
        } catch (Exception e) {
            log.error("{}异常，目标：{}", action, target, e);
            return SingleSendResult.fail(e.getMessage());
        }
    }

    private SingleSendResult parsePlatformResult(JsonNode root) {
        if (root == null || root.isNull()) {
            return SingleSendResult.fail("短信平台返回空响应");
        }
        String code = readText(root, "code");
        String message = readText(root, PLATFORM_MESSAGE_FIELD);
        if (StrUtil.isBlank(message)) {
            message = readText(root, "msg");
        }
        if (PLATFORM_SUCCESS_CODE.equals(code)) {
            return SingleSendResult.success(StrUtil.blankToDefault(message, "发送成功"));
        }
        String errorMsg = StrUtil.blankToDefault(message,
            "短信平台返回 code=" + StrUtil.blankToDefault(code, "空"));
        return SingleSendResult.fail(errorMsg);
    }

    private String readText(JsonNode root, String fieldName) {
        if (root == null || StrUtil.isBlank(fieldName)) {
            return null;
        }
        JsonNode node = root.findPath(fieldName);
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private EvacuationSmsResult buildResultAndRecord(String batchContent, List<PhoneSendResult> sendResults, SmsSendContext context) {
        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;
        String batchErrorMsg = null;
        List<String> failedPhones = new ArrayList<>();

        for (PhoneSendResult sendResult : sendResults) {
            SingleSendResult result = sendResult.result();
            if (result.skipped()) {
                skipCount++;
                if (batchErrorMsg == null) {
                    batchErrorMsg = result.errorMsg();
                }
            } else if (result.success()) {
                successCount++;
            } else {
                failCount++;
                failedPhones.add(sendResult.phone());
                if (batchErrorMsg == null) {
                    batchErrorMsg = result.errorMsg();
                }
            }
            if (isAuditContext(context)) {
                recordSmsSendStat(sendResult.phone(), sendResult.content(), result, context);
            }
        }

        EvacuationSmsResult result = EvacuationSmsResult.builder()
                                                        .successCount(successCount)
                                                        .failCount(failCount)
                                                        .skipCount(skipCount)
                                                        .failedPhones(failedPhones)
                                                        .totalCount(sendResults.size())
                                                        .build();
        if (isAuditContext(context)) {
            recordSmsSendBatch(batchContent, result, context, batchErrorMsg);
        }
        return result;
    }

    private boolean isAuditContext(SmsSendContext context) {
        return context != null
            && context.getBizType() != null
            && context.getBizId() != null
            && StringUtils.isNotBlank(context.getSmsType());
    }

    private String buildBatchContentSnapshot(List<PhoneSendResult> sendResults) {
        if (sendResults == null || sendResults.isEmpty()) {
            return null;
        }
        if (sendResults.size() == 1) {
            return sendResults.get(0).content();
        }
        return "点对点批量短信，共" + sendResults.size() + "条";
    }

    private String buildSignedContent(String content, SmsConfigSnapshot snapshot) {
        String text = content == null ? "" : content;
        String signature = StrUtil.trim(snapshot.getSignature());
        if (StrUtil.isBlank(signature) || text.contains(signature)) {
            return text;
        }
        return signature + text;
    }

    private String resolveDisplayMessage(SingleSendResult result) {
        if (result == null) {
            return DEFAULT_SMS_ERROR_MSG;
        }
        if (result.success()) {
            return StrUtil.blankToDefault(result.platformMessage(), "发送成功");
        }
        return StrUtil.blankToDefault(result.errorMsg(), DEFAULT_SMS_ERROR_MSG);
    }

    private String normalizePhone(String phone) {
        return phone == null ? null : phone.trim();
    }

    private Map<String, Object> buildBasePlatformBody(SmsConfigSnapshot snapshot) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("account", snapshot.getAccount());
        requestBody.put("pwd", snapshot.getPwd());
        return requestBody;
    }

    private String buildTaskId() {
        return "dz-" + UUID.randomUUID().toString().replace("-", "");
    }

    private String normalizeBaseUrl(SmsConfigSnapshot snapshot) {
        String value = StrUtil.trim(snapshot.getBaseUrl());
        while (StrUtil.isNotBlank(value) && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String normalizePath(String path) {
        String value = StrUtil.trim(path);
        if (StrUtil.isBlank(value)) {
            return "";
        }
        return value.startsWith("/") ? value : "/" + value;
    }

    private String validatePlatformConfig(SmsConfigSnapshot snapshot) {
        if (snapshot == null || StrUtil.isBlank(snapshot.getBaseUrl())) {
            return "短信平台 base-url 未配置";
        }
        if (StrUtil.isBlank(snapshot.getAccount())) {
            return "短信平台 account 未配置";
        }
        if (StrUtil.isBlank(snapshot.getPwd())) {
            return "短信平台 pwd 未配置";
        }
        return null;
    }

    private void validatePlatformConfigOrThrow(SmsConfigSnapshot snapshot) {
        String errorMsg = validatePlatformConfig(snapshot);
        if (errorMsg != null) {
            throw new ServiceException(errorMsg);
        }
    }

    private void prepareBatchContext(SmsSendContext context) {
        if (context == null) {
            return;
        }
        if (StrUtil.isBlank(context.getBatchId())) {
            context.setBatchId(UUID.randomUUID().toString());
        }
    }

    private void recordSmsSendStat(String phone, String content, SingleSendResult result, SmsSendContext context) {
        if (context == null) {
            return;
        }
        try {
            Date now = new Date();
            DzSmsSendStatBo bo = new DzSmsSendStatBo();
            bo.setBatchId(StrUtil.blankToDefault(context.getBatchId(), UUID.randomUUID().toString()));
            bo.setBizType(context.getBizType());
            bo.setBizId(context.getBizId());
            bo.setSmsType(context.getSmsType());
            bo.setSceneCode(context.getScene() == null ? null : context.getScene().name());
            bo.setTaskSourceType(context.getTaskSourceType());
            bo.setReceiverName(context.getReceiverName());
            bo.setReceiverPhone(phone);
            bo.setSmsContent(content);
            bo.setSendTime(now);
            bo.setCreateDate(now);
            bo.setUpdateDate(now);
            bo.setSendStatus(resolveSingleSendStatus(result));
            bo.setErrorMsg(result.success() ? null : StrUtil.blankToDefault(result.errorMsg(), DEFAULT_SMS_ERROR_MSG));
            dzSmsSendStatService.insertByBo(bo);
        } catch (Exception e) {
            log.warn("写入短信发送记录失败, phone={}, smsType={}", phone, context.getSmsType(), e);
        }
    }

    private void recordSmsSendBatch(String content, EvacuationSmsResult result, SmsSendContext context, String errorMsg) {
        if (context == null || result == null) {
            return;
        }
        try {
            int totalCount = result.getTotalCount();
            int failCount = result.getFailCount();
            int successCount = result.getSuccessCount();
            int skipCount = result.getSkipCount();
            Date now = new Date();
            DzSmsSendBatchBo bo = new DzSmsSendBatchBo();
            bo.setBatchId(context.getBatchId());
            bo.setBizType(context.getBizType());
            bo.setBizId(context.getBizId());
            bo.setSmsType(context.getSmsType());
            bo.setSceneCode(context.getScene() == null ? null : context.getScene().name());
            bo.setTaskSourceType(context.getTaskSourceType());
            bo.setTargetType(StrUtil.blankToDefault(context.getTargetType(), DEFAULT_BATCH_TARGET_TYPE));
            bo.setContentSnapshot(content);
            bo.setSendStatus(resolveBatchStatus(totalCount, successCount, failCount, skipCount));
            bo.setTotalCount(totalCount);
            bo.setSuccessCount(successCount);
            bo.setFailCount(failCount);
            bo.setSkipCount(skipCount);
            bo.setRequestTime(now);
            bo.setFinishTime(now);
            bo.setErrorMsg(resolveBatchErrorMsg(totalCount, successCount, failCount, skipCount, errorMsg));
            bo.setCreateDate(now);
            bo.setUpdateDate(now);
            dzSmsSendBatchService.insertByBo(bo);
        } catch (Exception e) {
            log.warn("写入短信发送批次失败, batchId={}, smsType={}", context.getBatchId(), context.getSmsType(), e);
        }
    }

    private String resolveSingleSendStatus(SingleSendResult result) {
        if (result.skipped()) {
            return DzSmsSendBatch.SEND_STATUS_SKIPPED;
        }
        return result.success() ? DzSmsSendBatch.SEND_STATUS_SUCCESS : DzSmsSendBatch.SEND_STATUS_FAIL;
    }

    private String resolveBatchStatus(int totalCount, int successCount, int failCount, int skipCount) {
        if (totalCount <= 0) {
            return DzSmsSendBatch.SEND_STATUS_INIT;
        }
        if (skipCount == totalCount) {
            return DzSmsSendBatch.SEND_STATUS_SKIPPED;
        }
        if (failCount <= 0 && skipCount <= 0) {
            return DzSmsSendBatch.SEND_STATUS_SUCCESS;
        }
        if (successCount <= 0 && skipCount <= 0) {
            return DzSmsSendBatch.SEND_STATUS_FAIL;
        }
        return DzSmsSendBatch.SEND_STATUS_PART_SUCCESS;
    }

    private String resolveBatchErrorMsg(int totalCount, int successCount, int failCount, int skipCount, String errorMsg) {
        if (totalCount <= 0) {
            return "无可发送对象";
        }
        if (skipCount == totalCount) {
            return StrUtil.blankToDefault(errorMsg, "短信配置关闭，本批次未发送");
        }
        if (failCount <= 0 && skipCount <= 0) {
            return null;
        }
        if (successCount <= 0 && skipCount <= 0) {
            return StrUtil.blankToDefault(errorMsg, DEFAULT_SMS_ERROR_MSG);
        }
        return StrUtil.blankToDefault(errorMsg, "部分短信发送失败或被配置跳过");
    }

    private record PhoneSendResult(String phone, String content, SingleSendResult result) {
    }

    private record BatchSendPayloadItem(String mobile, String content) {
    }

    private record SingleSendResult(boolean success, boolean skipped, String errorMsg, String platformMessage) {

        private static SingleSendResult success(String message) {
            return new SingleSendResult(true, false, null, message);
        }

        private static SingleSendResult fail(String errorMsg) {
            return new SingleSendResult(false, false, errorMsg, errorMsg);
        }

        private static SingleSendResult skipped(String reason) {
            return new SingleSendResult(false, true, reason, reason);
        }
    }
}
