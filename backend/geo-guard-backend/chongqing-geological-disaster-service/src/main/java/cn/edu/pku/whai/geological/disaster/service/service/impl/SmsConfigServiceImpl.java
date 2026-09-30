/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.SmsConfigUpdateBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSysConfig;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.SmsConfigVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzSysConfigMapper;
import cn.edu.pku.whai.geological.disaster.service.service.ISmsConfigService;
import cn.edu.pku.whai.geological.disaster.service.sms.config.SmsConfigSnapshot;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 短信动态配置服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsConfigServiceImpl implements ISmsConfigService {

    private static final String PREFIX = "dizai.sms.";
    private static final String KEY_PLATFORM_ENABLED = PREFIX + "platform.enabled";
    private static final String KEY_BASE_URL = PREFIX + "platform.base_url";
    private static final String KEY_ACCOUNT = PREFIX + "platform.account";
    private static final String KEY_PWD = PREFIX + "platform.pwd";
    private static final String KEY_SIGNATURE = PREFIX + "platform.signature";
    private static final String KEY_SEND_PATH = PREFIX + "platform.send_path";
    private static final String KEY_BATCH_SEND_PATH = PREFIX + "platform.batch_send_path";
    private static final String KEY_REPORT_PATH = PREFIX + "platform.report_path";
    private static final String KEY_DEDUP_MINUTES = PREFIX + "dedup_minutes";
    private static final String KEY_BUSINESS_ENABLED = PREFIX + "business.enabled";
    private static final String KEY_TASK_PUSH_ENABLED = PREFIX + "scene.task_push.enabled";
    private static final String KEY_TASK_PUSH_SOURCE_TYPES = PREFIX + "scene.task_push.allowed_source_types";
    private static final String KEY_EVACUATION_ENABLED = PREFIX + "scene.evacuation.enabled";
    private static final String KEY_DEF_RESP_START_ENABLED = PREFIX + "scene.def_resp_start.enabled";
    private static final String KEY_MEETING_INVITE_ENABLED = PREFIX + "scene.meeting_invite.enabled";
    private static final String KEY_ADMIN_APPROVAL_ENABLED = PREFIX + "scene.admin_approval.enabled";
    private static final String KEY_CAPTCHA_ENABLED = PREFIX + "auth.captcha.enabled";
    private static final String KEY_TEST_ENABLED = PREFIX + "test.enabled";
    private static final String KEY_UPDATED_BY = PREFIX + "updated_by";
    private static final String KEY_UPDATED_AT = PREFIX + "updated_at";
    private static final String LEGACY_TASK_SMS_KEY = "dizai.automation.notice.task_sms_enabled";

    private static final String CACHE_KEY = "dizai:sms:config";
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);
    private static final List<Integer> ALL_SOURCE_TYPES = List.of(0, 1, 2, 3, 4, 5, 6);
    private static final Set<Integer> VALID_SOURCE_TYPES = Set.copyOf(ALL_SOURCE_TYPES);
    private static final int CONFIG_VALUE_MAX_LENGTH = 500;

    private static final Map<String, String> CONFIG_NAMES = Map.ofEntries(
        Map.entry(KEY_PLATFORM_ENABLED, "短信平台总开关"),
        Map.entry(KEY_BASE_URL, "短信平台基础地址"),
        Map.entry(KEY_ACCOUNT, "短信平台账号"),
        Map.entry(KEY_PWD, "短信平台接口密码"),
        Map.entry(KEY_SIGNATURE, "短信签名"),
        Map.entry(KEY_SEND_PATH, "短信单条发送接口路径"),
        Map.entry(KEY_BATCH_SEND_PATH, "短信批量发送接口路径"),
        Map.entry(KEY_REPORT_PATH, "短信状态报告接口路径"),
        Map.entry(KEY_DEDUP_MINUTES, "短信重复发送拦截分钟数"),
        Map.entry(KEY_BUSINESS_ENABLED, "业务短信总开关"),
        Map.entry(KEY_TASK_PUSH_ENABLED, "任务推送短信开关"),
        Map.entry(KEY_TASK_PUSH_SOURCE_TYPES, "任务短信允许来源编码"),
        Map.entry(KEY_EVACUATION_ENABLED, "群众撤离短信开关"),
        Map.entry(KEY_DEF_RESP_START_ENABLED, "防御响应启动短信开关"),
        Map.entry(KEY_MEETING_INVITE_ENABLED, "会议邀请短信开关"),
        Map.entry(KEY_ADMIN_APPROVAL_ENABLED, "行政审批短信开关"),
        Map.entry(KEY_CAPTCHA_ENABLED, "短信验证码开关"),
        Map.entry(KEY_TEST_ENABLED, "测试短信开关"),
        Map.entry(KEY_UPDATED_BY, "短信配置最后修改人"),
        Map.entry(KEY_UPDATED_AT, "短信配置最后修改时间")
    );

    private final DzSysConfigMapper sysConfigMapper;
    private final AtomicReference<SmsConfigSnapshot> lastValidSnapshot = new AtomicReference<>();

    @Override
    public SmsConfigSnapshot getSnapshot() {
        try {
            SmsConfigSnapshot cached = RedisUtils.getCacheObject(CACHE_KEY);
            if (cached != null) {
                validateSnapshot(cached);
                lastValidSnapshot.set(cached);
                return cached;
            }
        } catch (Exception e) {
            log.warn("读取短信动态配置 Redis 缓存失败，将回源数据库, key={}", CACHE_KEY, e);
        }
        try {
            SmsConfigSnapshot loaded = loadSnapshotFromDb();
            publishSnapshot(loaded);
            return loaded;
        } catch (Exception e) {
            SmsConfigSnapshot fallback = lastValidSnapshot.get();
            if (fallback != null) {
                log.error("读取短信动态配置失败，使用进程内最近有效快照", e);
                return fallback;
            }
            log.error("读取短信动态配置失败且无历史有效快照，短信平台将安全停发", e);
            return SmsConfigSnapshot.safeDisabled();
        }
    }

    @Override
    public SmsConfigVo getConfig() {
        return toVo(getSnapshot());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SmsConfigVo updateConfig(SmsConfigUpdateBo bo) {
        if (bo == null) {
            throw new ServiceException("短信配置更新参数不能为空");
        }
        Map<String, DzSysConfig> currentConfigs = loadConfigMap();
        // 旧配置可能处于“已启用但参数不完整”的历史状态；更新时应先以原始值合并本次入参，
        // 再校验最终快照，确保可以通过本接口关闭平台或补齐缺失参数。
        SmsConfigSnapshot current = buildSnapshot(currentConfigs, false);
        SmsConfigSnapshot merged = mergeAndValidate(current, bo);
        Date now = new Date();
        Long userId = resolveCurrentUserId();

        saveSuppliedFields(currentConfigs, bo, merged, userId, now);
        saveConfig(currentConfigs, KEY_UPDATED_BY, userId == null ? "" : String.valueOf(userId), userId, now);
        saveConfig(currentConfigs, KEY_UPDATED_AT, DateUtil.formatDateTime(now), userId, now);

        SmsConfigSnapshot result = merged.toBuilder().updatedBy(userId).updatedAt(now).build();
        publishAfterCommit();
        return toVo(result);
    }

    @Override
    public SmsConfigSnapshot refreshCache() {
        try {
            RedisUtils.deleteObject(CACHE_KEY);
        } catch (Exception e) {
            log.warn("删除短信动态配置 Redis 缓存失败, key={}", CACHE_KEY, e);
        }
        try {
            SmsConfigSnapshot loaded = loadSnapshotFromDb();
            publishSnapshot(loaded);
            return loaded;
        } catch (Exception e) {
            SmsConfigSnapshot fallback = lastValidSnapshot.get();
            if (fallback != null) {
                log.error("刷新短信动态配置失败，继续使用进程内最近有效快照", e);
                return fallback;
            }
            log.error("刷新短信动态配置失败且无历史有效快照，短信平台将安全停发", e);
            return SmsConfigSnapshot.safeDisabled();
        }
    }

    private SmsConfigSnapshot loadSnapshotFromDb() {
        return buildSnapshot(loadConfigMap());
    }

    private Map<String, DzSysConfig> loadConfigMap() {
        List<DzSysConfig> configs = sysConfigMapper.selectList(
            Wrappers.<DzSysConfig>lambdaQuery()
                .likeRight(DzSysConfig::getConfigKey, PREFIX)
                .orderByDesc(DzSysConfig::getUpdateTime, DzSysConfig::getConfigId)
        );
        Map<String, DzSysConfig> result = new LinkedHashMap<>();
        for (DzSysConfig config : configs) {
            if (config != null && config.getConfigKey() != null) {
                DzSysConfig duplicate = result.putIfAbsent(config.getConfigKey(), config);
                if (duplicate != null) {
                    log.warn("短信动态配置存在重复 key，使用最新记录, key={}, selectedId={}, ignoredId={}",
                        config.getConfigKey(), duplicate.getConfigId(), config.getConfigId());
                }
            }
        }
        return result;
    }

    private SmsConfigSnapshot buildSnapshot(Map<String, DzSysConfig> configs) {
        return buildSnapshot(configs, true);
    }

    private SmsConfigSnapshot buildSnapshot(Map<String, DzSysConfig> configs, boolean validate) {
        SmsConfigSnapshot snapshot = SmsConfigSnapshot.builder()
            .platformEnabled(readBoolean(configs, KEY_PLATFORM_ENABLED, false))
            .baseUrl(readString(configs, KEY_BASE_URL, ""))
            .account(readString(configs, KEY_ACCOUNT, ""))
            .pwd(readString(configs, KEY_PWD, ""))
            .signature(readString(configs, KEY_SIGNATURE, ""))
            .sendPath(readString(configs, KEY_SEND_PATH, "/api/v1/send"))
            .batchSendPath(readString(configs, KEY_BATCH_SEND_PATH, "/api/v1/batchSend"))
            .reportPath(readString(configs, KEY_REPORT_PATH, "/api/v1/report"))
            .dedupMinutes(readNonNegativeInt(configs, KEY_DEDUP_MINUTES, 1))
            .businessEnabled(readBoolean(configs, KEY_BUSINESS_ENABLED, true))
            .taskPushEnabled(readBoolean(configs, KEY_TASK_PUSH_ENABLED, true))
            .taskPushAllowedSourceTypes(readSourceTypes(configs))
            .evacuationEnabled(readBoolean(configs, KEY_EVACUATION_ENABLED, true))
            .defRespStartEnabled(readBoolean(configs, KEY_DEF_RESP_START_ENABLED, true))
            .meetingInviteEnabled(readBoolean(configs, KEY_MEETING_INVITE_ENABLED, true))
            .adminApprovalEnabled(readBoolean(configs, KEY_ADMIN_APPROVAL_ENABLED, true))
            .captchaEnabled(readBoolean(configs, KEY_CAPTCHA_ENABLED, true))
            .testEnabled(readBoolean(configs, KEY_TEST_ENABLED, true))
            .updatedBy(readLong(configs, KEY_UPDATED_BY))
            .updatedAt(readDate(configs, KEY_UPDATED_AT))
            .build();
        if (validate) {
            validateSnapshot(snapshot);
        }
        return snapshot;
    }

    private SmsConfigSnapshot mergeAndValidate(SmsConfigSnapshot current, SmsConfigUpdateBo bo) {
        SmsConfigSnapshot.SmsConfigSnapshotBuilder builder = current.toBuilder();
        if (bo.getPlatformEnabled() != null) {
            builder.platformEnabled(bo.getPlatformEnabled());
        }
        if (bo.getBaseUrl() != null) {
            builder.baseUrl(normalizeText(bo.getBaseUrl(), "平台基础地址"));
        }
        if (bo.getAccount() != null) {
            builder.account(normalizeText(bo.getAccount(), "平台账号"));
        }
        if (bo.getPwd() != null) {
            builder.pwd(normalizeText(bo.getPwd(), "平台接口密码"));
        }
        if (bo.getSignature() != null) {
            builder.signature(normalizeText(bo.getSignature(), "短信签名"));
        }
        if (bo.getSendPath() != null) {
            builder.sendPath(normalizeText(bo.getSendPath(), "单条发送接口路径"));
        }
        if (bo.getBatchSendPath() != null) {
            builder.batchSendPath(normalizeText(bo.getBatchSendPath(), "批量发送接口路径"));
        }
        if (bo.getReportPath() != null) {
            builder.reportPath(normalizeText(bo.getReportPath(), "状态报告接口路径"));
        }
        if (bo.getDedupMinutes() != null) {
            builder.dedupMinutes(bo.getDedupMinutes());
        }
        if (bo.getBusinessEnabled() != null) {
            builder.businessEnabled(bo.getBusinessEnabled());
        }
        if (bo.getTaskPushEnabled() != null) {
            builder.taskPushEnabled(bo.getTaskPushEnabled());
        }
        if (bo.getTaskPushAllowedSourceTypes() != null) {
            builder.taskPushAllowedSourceTypes(normalizeSourceTypes(bo.getTaskPushAllowedSourceTypes()));
        }
        if (bo.getEvacuationEnabled() != null) {
            builder.evacuationEnabled(bo.getEvacuationEnabled());
        }
        if (bo.getDefRespStartEnabled() != null) {
            builder.defRespStartEnabled(bo.getDefRespStartEnabled());
        }
        if (bo.getMeetingInviteEnabled() != null) {
            builder.meetingInviteEnabled(bo.getMeetingInviteEnabled());
        }
        if (bo.getAdminApprovalEnabled() != null) {
            builder.adminApprovalEnabled(bo.getAdminApprovalEnabled());
        }
        if (bo.getCaptchaEnabled() != null) {
            builder.captchaEnabled(bo.getCaptchaEnabled());
        }
        if (bo.getTestEnabled() != null) {
            builder.testEnabled(bo.getTestEnabled());
        }
        SmsConfigSnapshot merged = builder.build();
        validateSnapshot(merged);
        return merged;
    }

    private void validateSnapshot(SmsConfigSnapshot snapshot) {
        if (snapshot == null) {
            throw new ServiceException("短信配置快照不能为空");
        }
        validateOptionalBaseUrl(snapshot.getBaseUrl());
        validatePath(snapshot.getSendPath(), "单条发送接口路径");
        validatePath(snapshot.getBatchSendPath(), "批量发送接口路径");
        validatePath(snapshot.getReportPath(), "状态报告接口路径");
        if (snapshot.getDedupMinutes() < 0) {
            throw new ServiceException("短信去重分钟数不能小于 0");
        }
        normalizeSourceTypes(snapshot.getTaskPushAllowedSourceTypes());
        validateLength(snapshot.getAccount(), "平台账号");
        validateLength(snapshot.getPwd(), "平台接口密码");
        validateLength(snapshot.getSignature(), "短信签名");
        if (snapshot.isPlatformEnabled()) {
            if (isBlank(snapshot.getBaseUrl())) {
                throw new ServiceException("开启短信平台前必须配置平台基础地址");
            }
            if (isBlank(snapshot.getAccount())) {
                throw new ServiceException("开启短信平台前必须配置平台账号");
            }
            if (isBlank(snapshot.getPwd())) {
                throw new ServiceException("开启短信平台前必须配置平台接口密码");
            }
            if (isBlank(snapshot.getSendPath())) {
                throw new ServiceException("开启短信平台前必须配置单条发送接口路径");
            }
        }
    }

    private void saveSuppliedFields(Map<String, DzSysConfig> currentConfigs, SmsConfigUpdateBo bo,
                                    SmsConfigSnapshot merged, Long userId, Date now) {
        saveIfSupplied(currentConfigs, KEY_PLATFORM_ENABLED, bo.getPlatformEnabled(), merged.isPlatformEnabled(), userId, now);
        saveIfSupplied(currentConfigs, KEY_BASE_URL, bo.getBaseUrl(), merged.getBaseUrl(), userId, now);
        saveIfSupplied(currentConfigs, KEY_ACCOUNT, bo.getAccount(), merged.getAccount(), userId, now);
        saveIfSupplied(currentConfigs, KEY_PWD, bo.getPwd(), merged.getPwd(), userId, now);
        saveIfSupplied(currentConfigs, KEY_SIGNATURE, bo.getSignature(), merged.getSignature(), userId, now);
        saveIfSupplied(currentConfigs, KEY_SEND_PATH, bo.getSendPath(), merged.getSendPath(), userId, now);
        saveIfSupplied(currentConfigs, KEY_BATCH_SEND_PATH, bo.getBatchSendPath(), merged.getBatchSendPath(), userId, now);
        saveIfSupplied(currentConfigs, KEY_REPORT_PATH, bo.getReportPath(), merged.getReportPath(), userId, now);
        saveIfSupplied(currentConfigs, KEY_DEDUP_MINUTES, bo.getDedupMinutes(), merged.getDedupMinutes(), userId, now);
        saveIfSupplied(currentConfigs, KEY_BUSINESS_ENABLED, bo.getBusinessEnabled(), merged.isBusinessEnabled(), userId, now);
        saveIfSupplied(currentConfigs, KEY_TASK_PUSH_ENABLED, bo.getTaskPushEnabled(), merged.isTaskPushEnabled(), userId, now);
        if (bo.getTaskPushEnabled() != null) {
            saveLegacyTaskSmsConfig(merged.isTaskPushEnabled(), userId, now);
        }
        if (bo.getTaskPushAllowedSourceTypes() != null) {
            saveConfig(currentConfigs, KEY_TASK_PUSH_SOURCE_TYPES,
                merged.getTaskPushAllowedSourceTypes().stream().map(String::valueOf).collect(Collectors.joining(",")), userId, now);
        }
        saveIfSupplied(currentConfigs, KEY_EVACUATION_ENABLED, bo.getEvacuationEnabled(), merged.isEvacuationEnabled(), userId, now);
        saveIfSupplied(currentConfigs, KEY_DEF_RESP_START_ENABLED, bo.getDefRespStartEnabled(), merged.isDefRespStartEnabled(), userId, now);
        saveIfSupplied(currentConfigs, KEY_MEETING_INVITE_ENABLED, bo.getMeetingInviteEnabled(), merged.isMeetingInviteEnabled(), userId, now);
        saveIfSupplied(currentConfigs, KEY_ADMIN_APPROVAL_ENABLED, bo.getAdminApprovalEnabled(), merged.isAdminApprovalEnabled(), userId, now);
        saveIfSupplied(currentConfigs, KEY_CAPTCHA_ENABLED, bo.getCaptchaEnabled(), merged.isCaptchaEnabled(), userId, now);
        saveIfSupplied(currentConfigs, KEY_TEST_ENABLED, bo.getTestEnabled(), merged.isTestEnabled(), userId, now);
    }

    private void saveIfSupplied(Map<String, DzSysConfig> configs, String key, Object supplied,
                                Object normalized, Long userId, Date now) {
        if (supplied != null) {
            saveConfig(configs, key, String.valueOf(normalized), userId, now);
        }
    }

    private void saveConfig(Map<String, DzSysConfig> configs, String key, String value, Long userId, Date now) {
        validateLength(value, CONFIG_NAMES.getOrDefault(key, key));
        DzSysConfig current = configs.get(key);
        if (current == null) {
            DzSysConfig insert = new DzSysConfig();
            insert.setConfigId(IdUtil.getSnowflakeNextId());
            insert.setTenantId("000000");
            insert.setConfigName(CONFIG_NAMES.getOrDefault(key, key));
            insert.setConfigKey(key);
            insert.setConfigValue(value);
            insert.setConfigType("N");
            insert.setCreateBy(userId);
            insert.setCreateTime(now);
            insert.setUpdateBy(userId);
            insert.setUpdateTime(now);
            insert.setRemark("地灾短信动态配置");
            sysConfigMapper.insert(insert);
            configs.put(key, insert);
            return;
        }
        DzSysConfig update = new DzSysConfig();
        update.setConfigId(current.getConfigId());
        update.setConfigName(CONFIG_NAMES.getOrDefault(key, current.getConfigName()));
        update.setConfigValue(value);
        update.setUpdateBy(userId);
        update.setUpdateTime(now);
        sysConfigMapper.updateById(update);
        current.setConfigValue(value);
        current.setUpdateBy(userId);
        current.setUpdateTime(now);
    }

    private void saveLegacyTaskSmsConfig(boolean enabled, Long userId, Date now) {
        DzSysConfig legacy = sysConfigMapper.selectOne(
            Wrappers.<DzSysConfig>lambdaQuery()
                .eq(DzSysConfig::getConfigKey, LEGACY_TASK_SMS_KEY)
                .last("limit 1")
        );
        if (legacy == null) {
            DzSysConfig insert = new DzSysConfig();
            insert.setConfigId(IdUtil.getSnowflakeNextId());
            insert.setTenantId("000000");
            insert.setConfigName("普通任务短信自动发送");
            insert.setConfigKey(LEGACY_TASK_SMS_KEY);
            insert.setConfigValue(String.valueOf(enabled));
            insert.setConfigType("N");
            insert.setCreateBy(userId);
            insert.setCreateTime(now);
            insert.setUpdateBy(userId);
            insert.setUpdateTime(now);
            insert.setRemark("兼容旧自动化运营配置，运行时以短信动态配置为准");
            sysConfigMapper.insert(insert);
            return;
        }
        DzSysConfig update = new DzSysConfig();
        update.setConfigId(legacy.getConfigId());
        update.setConfigValue(String.valueOf(enabled));
        update.setUpdateBy(userId);
        update.setUpdateTime(now);
        sysConfigMapper.updateById(update);
    }

    private void publishAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            refreshCache();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                refreshCache();
            }
        });
    }

    private void publishSnapshot(SmsConfigSnapshot snapshot) {
        lastValidSnapshot.set(snapshot);
        try {
            RedisUtils.deleteObject(CACHE_KEY);
            RedisUtils.setCacheObject(CACHE_KEY, snapshot, CACHE_TTL);
        } catch (Exception e) {
            log.warn("刷新短信动态配置 Redis 缓存失败，进程内快照仍已更新, key={}", CACHE_KEY, e);
        }
    }

    private SmsConfigVo toVo(SmsConfigSnapshot snapshot) {
        SmsConfigVo vo = new SmsConfigVo();
        vo.setPlatformEnabled(snapshot.isPlatformEnabled());
        vo.setBaseUrl(snapshot.getBaseUrl());
        vo.setAccountConfigured(!isBlank(snapshot.getAccount()));
        vo.setPwdConfigured(!isBlank(snapshot.getPwd()));
        vo.setSignature(snapshot.getSignature());
        vo.setSendPath(snapshot.getSendPath());
        vo.setBatchSendPath(snapshot.getBatchSendPath());
        vo.setReportPath(snapshot.getReportPath());
        vo.setDedupMinutes(snapshot.getDedupMinutes());
        vo.setBusinessEnabled(snapshot.isBusinessEnabled());
        vo.setTaskPushEnabled(snapshot.isTaskPushEnabled());
        vo.setTaskPushAllowedSourceTypes(List.copyOf(snapshot.getTaskPushAllowedSourceTypes()));
        vo.setEvacuationEnabled(snapshot.isEvacuationEnabled());
        vo.setDefRespStartEnabled(snapshot.isDefRespStartEnabled());
        vo.setMeetingInviteEnabled(snapshot.isMeetingInviteEnabled());
        vo.setAdminApprovalEnabled(snapshot.isAdminApprovalEnabled());
        vo.setCaptchaEnabled(snapshot.isCaptchaEnabled());
        vo.setTestEnabled(snapshot.isTestEnabled());
        vo.setUpdatedBy(snapshot.getUpdatedBy());
        vo.setUpdatedAt(snapshot.getUpdatedAt());
        return vo;
    }

    private boolean readBoolean(Map<String, DzSysConfig> configs, String key, boolean defaultValue) {
        String value = valueOf(configs, key);
        if (isBlank(value)) {
            return defaultValue;
        }
        String normalized = value.trim();
        if (!"true".equalsIgnoreCase(normalized) && !"false".equalsIgnoreCase(normalized)) {
            throw new ServiceException(CONFIG_NAMES.getOrDefault(key, key) + "必须为 true 或 false");
        }
        return Boolean.parseBoolean(normalized);
    }

    private int readNonNegativeInt(Map<String, DzSysConfig> configs, String key, int defaultValue) {
        String value = valueOf(configs, key);
        if (isBlank(value)) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 0) {
                throw new ServiceException(CONFIG_NAMES.getOrDefault(key, key) + "不能小于 0");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new ServiceException(CONFIG_NAMES.getOrDefault(key, key) + "必须为非负整数");
        }
    }

    private List<Integer> readSourceTypes(Map<String, DzSysConfig> configs) {
        if (!configs.containsKey(KEY_TASK_PUSH_SOURCE_TYPES)) {
            return ALL_SOURCE_TYPES;
        }
        String value = valueOf(configs, KEY_TASK_PUSH_SOURCE_TYPES);
        if (isBlank(value)) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>();
        for (String item : value.split(",", -1)) {
            try {
                result.add(Integer.parseInt(item.trim()));
            } catch (NumberFormatException e) {
                throw new ServiceException("任务短信允许来源编码必须为 0～6 的整数");
            }
        }
        return normalizeSourceTypes(result);
    }

    private List<Integer> normalizeSourceTypes(List<Integer> sourceTypes) {
        if (sourceTypes == null) {
            throw new ServiceException("任务短信允许来源编码不能为空");
        }
        if (sourceTypes.stream().anyMatch(Objects::isNull)) {
            throw new ServiceException("任务短信允许来源编码不能包含空值");
        }
        if (sourceTypes.stream().anyMatch(sourceType -> !VALID_SOURCE_TYPES.contains(sourceType))) {
            throw new ServiceException("任务短信允许来源编码只能为 0～6");
        }
        return sourceTypes.stream().distinct().sorted().toList();
    }

    private Long readLong(Map<String, DzSysConfig> configs, String key) {
        String value = valueOf(configs, key);
        if (isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new ServiceException(CONFIG_NAMES.getOrDefault(key, key) + "格式不正确");
        }
    }

    private Date readDate(Map<String, DzSysConfig> configs, String key) {
        String value = valueOf(configs, key);
        if (isBlank(value)) {
            return null;
        }
        try {
            return DateUtil.parseDateTime(value.trim());
        } catch (Exception e) {
            throw new ServiceException(CONFIG_NAMES.getOrDefault(key, key) + "格式不正确");
        }
    }

    private String readString(Map<String, DzSysConfig> configs, String key, String defaultValue) {
        if (!configs.containsKey(key)) {
            return defaultValue;
        }
        String value = valueOf(configs, key);
        return value == null ? "" : value.trim();
    }

    private String valueOf(Map<String, DzSysConfig> configs, String key) {
        DzSysConfig config = configs.get(key);
        return config == null ? null : config.getConfigValue();
    }

    private String normalizeText(String value, String fieldName) {
        String normalized = value.trim();
        validateLength(normalized, fieldName);
        return normalized;
    }

    private void validateOptionalBaseUrl(String value) {
        validateLength(value, "平台基础地址");
        if (isBlank(value)) {
            return;
        }
        try {
            URI uri = URI.create(value.trim());
            boolean validScheme = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
            if (!validScheme || isBlank(uri.getHost()) || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new ServiceException("平台基础地址必须是合法的 http 或 https URL，且不能包含账号、查询参数或片段");
        }
    }

    private void validatePath(String value, String fieldName) {
        validateLength(value, fieldName);
        if (isBlank(value)) {
            throw new ServiceException(fieldName + "不能为空");
        }
        try {
            URI uri = URI.create(value.trim());
            if (!value.startsWith("/") || value.startsWith("//") || uri.isAbsolute()
                || uri.getRawAuthority() != null || uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new ServiceException(fieldName + "必须是以 / 开头且不含域名、查询参数或片段的接口路径");
        }
    }

    private void validateLength(String value, String fieldName) {
        if (value != null && value.length() > CONFIG_VALUE_MAX_LENGTH) {
            throw new ServiceException(fieldName + "长度不能超过 " + CONFIG_VALUE_MAX_LENGTH + " 个字符");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Long resolveCurrentUserId() {
        try {
            return LoginHelper.getUserId();
        } catch (Exception ignored) {
            return null;
        }
    }
}
