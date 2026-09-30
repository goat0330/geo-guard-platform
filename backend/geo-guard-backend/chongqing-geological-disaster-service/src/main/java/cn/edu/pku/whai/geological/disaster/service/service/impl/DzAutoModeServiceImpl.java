/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.consts.AutoModeExecutionActions;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.HandleProcessEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.AutoModeConfigBo;
import cn.edu.pku.whai.geological.disaster.service.domain.dto.AutoModeTraceContext;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSysConfig;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AutoModeStatusVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzSysConfigMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutoModeAiHostingRecordService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutoModeExecutionTraceService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutoModeService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleService;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 地灾自动模式 Service。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DzAutoModeServiceImpl implements IDzAutoModeService {

    private static final String KEY_ENABLED = "dizai.auto_mode.enabled";
    private static final String KEY_STEP_INTERVAL_SECONDS = "dizai.auto_mode.step_interval_seconds";
    private static final String KEY_ACTOR_NAME = "dizai.auto_mode.actor_name";
    private static final String KEY_ACTOR_USER_ID = "dizai.auto_mode.actor_user_id";
    private static final String KEY_UPDATED_BY = "dizai.auto_mode.updated_by";
    private static final String KEY_UPDATED_AT = "dizai.auto_mode.updated_at";
    private static final String KEY_OPENED_AT = "dizai.auto_mode.opened_at";
    private static final String KEY_CLOSED_AT = "dizai.auto_mode.closed_at";
    private static final String CACHE_KEY = "dizai:auto-mode:config";
    private static final String RUNNER_LOCK_KEY = "dizai:auto-mode:runner";
    private static final String HANDLE_LOCK_PREFIX = "dizai:auto-mode:handle:";
    private static final String HANDLE_SKIP_PREFIX = "dizai:auto-mode:skip:";
    private static final String DEFAULT_ACTOR_NAME = "地象大模型";
    private static final int DEFAULT_STEP_INTERVAL_SECONDS = 30;
    private static final int MAX_HANDLE_PER_RUN = 20;

    private final DzSysConfigMapper sysConfigMapper;
    private final DzTaskHandleMapper dzTaskHandleMapper;
    private final IDzTaskHandleService dzTaskHandleService;
    private final IDzAutoModeExecutionTraceService executionTraceService;
    private final IDzAutoModeAiHostingRecordService aiHostingRecordService;

    @Override
    public AutoModeStatusVo getStatus() {
        try {
            AutoModeStatusVo cached = RedisUtils.getCacheObject(CACHE_KEY);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("自动模式配置缓存反序列化失败，删除旧缓存并回源重建, key={}", CACHE_KEY, e);
            RedisUtils.deleteObject(CACHE_KEY);
        }
        AutoModeStatusVo status = loadStatusFromDb();
        cacheStatus(status);
        return status;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AutoModeStatusVo open(AutoModeConfigBo bo) {
        AutoModeConfigBo effectiveBo = bo == null ? new AutoModeConfigBo() : bo;
        if (effectiveBo.getStepIntervalSeconds() != null && effectiveBo.getStepIntervalSeconds() < 1) {
            throw new ServiceException("步骤间隔必须大于0秒");
        }
        Boolean enabledBefore = Boolean.parseBoolean(Objects.toString(getConfigValue(KEY_ENABLED), "false"));
        String openedAtBefore = getConfigValue(KEY_OPENED_AT);
        Date openedAt = new Date();
        if (shouldWriteOpenedAtOnOpen(enabledBefore, openedAtBefore)) {
            saveConfig(KEY_OPENED_AT, DateUtil.formatDateTime(openedAt), "自动模式最近一次开启时间");
            aiHostingRecordService.createOnOpen(openedAt);
        }
        saveConfig(KEY_CLOSED_AT, "", "自动模式最近一次关闭时间");
        saveConfig(KEY_ENABLED, "true", "自动模式开关");
        applyConfig(effectiveBo);
        saveUpdateMeta();
        return refreshCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AutoModeStatusVo close() {
        Date closedAt = new Date();
        saveConfig(KEY_ENABLED, "false", "自动模式开关");
        saveConfig(KEY_CLOSED_AT, DateUtil.formatDateTime(closedAt), "自动模式最近一次关闭时间");
        aiHostingRecordService.closeLatest(closedAt);
        saveUpdateMeta();
        return refreshCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AutoModeStatusVo updateConfig(AutoModeConfigBo bo) {
        if (bo == null) {
            throw new ServiceException("自动模式配置不能为空");
        }
        if (bo.getStepIntervalSeconds() == null && StringUtils.isBlank(bo.getActorName()) && bo.getActorUserId() == null) {
            throw new ServiceException("未提交任何可修改配置");
        }
        if (bo.getStepIntervalSeconds() != null && bo.getStepIntervalSeconds() < 1) {
            throw new ServiceException("步骤间隔必须大于0秒");
        }
        applyConfig(bo);
        saveUpdateMeta();
        return refreshCache();
    }

    @Override
    public void runOnce() {
        AutoModeStatusVo status = getStatus();
        if (!Boolean.TRUE.equals(status.getEnabled())) {
            return;
        }
        RLock runnerLock = RedisUtils.getClient().getLock(RUNNER_LOCK_KEY);
        boolean locked = false;
        try {
            locked = runnerLock.tryLock(0, lockLeaseSeconds(status), TimeUnit.SECONDS);
            if (!locked) {
                return;
            }
            runEligibleHandles(status);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("自动模式调度获取锁被中断", e);
        } catch (Exception e) {
            log.error("自动模式调度执行失败", e);
        } finally {
            if (locked && runnerLock.isHeldByCurrentThread()) {
                runnerLock.unlock();
            }
        }
    }

    private void runEligibleHandles(AutoModeStatusVo status) {
        Date threshold = new Date(System.currentTimeMillis() - status.getStepIntervalSeconds() * 1000L);
        List<DzTaskHandle> handles = dzTaskHandleMapper.selectList(
            Wrappers.<DzTaskHandle>lambdaQuery()
                    .lt(DzTaskHandle::getHandleProcess, HandleProcessEnum.CONSULTATION_JUDGMENT.getCode())
                    .and(w -> w.le(DzTaskHandle::getUpdateDate, threshold)
                               .or(inner -> inner.isNull(DzTaskHandle::getUpdateDate)
                                                  .le(DzTaskHandle::getCreateDate, threshold)))
                    .orderByAsc(DzTaskHandle::getUpdateDate, DzTaskHandle::getCreateDate, DzTaskHandle::getId)
                    .last("limit " + MAX_HANDLE_PER_RUN)
        );
        for (DzTaskHandle handle : handles) {
            if (!Boolean.TRUE.equals(getStatus().getEnabled())) {
                return;
            }
            processOneHandle(handle, status);
        }
    }

    private void processOneHandle(DzTaskHandle handle, AutoModeStatusVo status) {
        if (handle == null || handle.getId() == null) {
            return;
        }
        String skipKey = HANDLE_SKIP_PREFIX + handle.getId();
        if (RedisUtils.hasKey(skipKey)) {
            return;
        }
        RLock handleLock = RedisUtils.getClient().getLock(HANDLE_LOCK_PREFIX + handle.getId());
        boolean locked = false;
        AutoModeTraceContext traceContext = null;
        Long traceId = null;
        try {
            locked = handleLock.tryLock(0, lockLeaseSeconds(status), TimeUnit.SECONDS);
            if (!locked) {
                return;
            }
            traceContext = buildHandleTraceContext(handle, status);
            traceId = executionTraceService.start(traceContext);
            Integer nextProcess = dzTaskHandleService.autoProcessNext(handle.getId(), status.getActorUserId(), status.getActorName());
            Map<String, Object> successDetail = new LinkedHashMap<>();
            successDetail.put("nextProcess", nextProcess);
            executionTraceService.success(traceId, traceContext, 1, successDetail);
            log.info("自动模式推进处置流程成功, handleId={}, nextProcess={}, actor={}", handle.getId(), nextProcess, status.getActorName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("自动模式处置任务获取锁被中断, handleId={}", handle.getId(), e);
        } catch (ServiceException e) {
            RedisUtils.setCacheObject(skipKey, e.getMessage(), Duration.ofSeconds(status.getStepIntervalSeconds()));
            executionTraceService.fail(traceId, traceContext, e, failureDetail(skipKey));
            log.info("自动模式处置任务暂不推进, handleId={}, reason={}", handle.getId(), e.getMessage());
        } catch (Exception e) {
            RedisUtils.setCacheObject(skipKey, e.getMessage(), Duration.ofSeconds(status.getStepIntervalSeconds()));
            executionTraceService.fail(traceId, traceContext, e, failureDetail(skipKey));
            log.error("自动模式处置任务推进失败, handleId={}", handle.getId(), e);
        } finally {
            if (locked && handleLock.isHeldByCurrentThread()) {
                handleLock.unlock();
            }
        }
    }

    private AutoModeTraceContext buildHandleTraceContext(DzTaskHandle handle, AutoModeStatusVo status) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("actorUserId", status == null ? null : status.getActorUserId());
        detail.put("actorName", status == null ? null : status.getActorName());
        detail.put("currentProcess", handle == null ? null : handle.getHandleProcess());
        return AutoModeTraceContext.builder()
            .actionType(AutoModeExecutionActions.DISPOSAL_AUTO_ADVANCE_CODE)
            .actionName(AutoModeExecutionActions.DISPOSAL_AUTO_ADVANCE_NAME)
            .bizType(TaskProcessBizTypeEnum.HANDLE.getCode())
            .bizId(handle == null ? null : handle.getId())
            .batchFlag(false)
            .executeCount(0)
            .detailJson(detail)
            .build();
    }

    private Map<String, Object> failureDetail(String skipKey) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("skipKey", skipKey);
        return detail;
    }

    private void applyConfig(AutoModeConfigBo bo) {
        if (bo.getStepIntervalSeconds() != null) {
            saveConfig(KEY_STEP_INTERVAL_SECONDS, String.valueOf(bo.getStepIntervalSeconds()), "自动模式步骤间隔秒数");
        }
        if (StringUtils.isNotBlank(bo.getActorName())) {
            saveConfig(KEY_ACTOR_NAME, bo.getActorName().trim(), "自动模式执行人名称");
        } else if (StringUtils.isBlank(getConfigValue(KEY_ACTOR_NAME))) {
            saveConfig(KEY_ACTOR_NAME, DEFAULT_ACTOR_NAME, "自动模式执行人名称");
        }
        if (bo.getActorUserId() != null) {
            saveConfig(KEY_ACTOR_USER_ID, String.valueOf(bo.getActorUserId()), "自动模式执行人用户ID");
        }
        if (StringUtils.isBlank(getConfigValue(KEY_STEP_INTERVAL_SECONDS))) {
            saveConfig(KEY_STEP_INTERVAL_SECONDS, String.valueOf(DEFAULT_STEP_INTERVAL_SECONDS), "自动模式步骤间隔秒数");
        }
    }

    private long lockLeaseSeconds(AutoModeStatusVo status) {
        int interval = status == null || status.getStepIntervalSeconds() == null
            ? DEFAULT_STEP_INTERVAL_SECONDS
            : status.getStepIntervalSeconds();
        return Math.max(60L, interval * 2L);
    }

    private AutoModeStatusVo loadStatusFromDb() {
        AutoModeStatusVo status = new AutoModeStatusVo();
        status.setEnabled(Boolean.parseBoolean(Objects.toString(getConfigValue(KEY_ENABLED), "false")));
        status.setStepIntervalSeconds(parseInt(getConfigValue(KEY_STEP_INTERVAL_SECONDS), DEFAULT_STEP_INTERVAL_SECONDS));
        status.setActorName(StringUtils.blankToDefault(getConfigValue(KEY_ACTOR_NAME), DEFAULT_ACTOR_NAME));
        status.setActorUserId(parseLong(getConfigValue(KEY_ACTOR_USER_ID), null));
        status.setUpdatedBy(parseLong(getConfigValue(KEY_UPDATED_BY), null));
        Date updatedAt = parseDate(getConfigValue(KEY_UPDATED_AT));
        status.setUpdatedAt(updatedAt);
        Date openedAt = parseDate(getConfigValue(KEY_OPENED_AT));
        status.setOpenedAt(openedAt == null ? updatedAt : openedAt);
        status.setClosedAt(parseDate(getConfigValue(KEY_CLOSED_AT)));
        return status;
    }

    private AutoModeStatusVo refreshCache() {
        RedisUtils.deleteObject(CACHE_KEY);
        AutoModeStatusVo status = loadStatusFromDb();
        cacheStatus(status);
        return status;
    }

    private void cacheStatus(AutoModeStatusVo status) {
        RedisUtils.setCacheObject(CACHE_KEY, status, Duration.ofSeconds(30));
    }

    private String getConfigValue(String key) {
        DzSysConfig config = getConfig(key);
        return config == null ? null : config.getConfigValue();
    }

    private DzSysConfig getConfig(String key) {
        return sysConfigMapper.selectOne(
            Wrappers.<DzSysConfig>lambdaQuery()
                    .eq(DzSysConfig::getConfigKey, key)
                    .last("limit 1")
        );
    }

    private void saveUpdateMeta() {
        Long userId = resolveCurrentUserId();
        saveConfig(KEY_UPDATED_BY, userId == null ? "" : String.valueOf(userId), "自动模式最后修改人");
        saveConfig(KEY_UPDATED_AT, DateUtil.formatDateTime(new Date()), "自动模式最后修改时间");
    }

    private void saveConfig(String key, String value, String name) {
        Date now = new Date();
        Long userId = resolveCurrentUserId();
        DzSysConfig current = getConfig(key);
        if (current == null) {
            DzSysConfig insert = new DzSysConfig();
            insert.setConfigId(IdUtil.getSnowflakeNextId());
            insert.setTenantId("000000");
            insert.setConfigName(name);
            insert.setConfigKey(key);
            insert.setConfigValue(value);
            insert.setConfigType("N");
            insert.setCreateBy(userId);
            insert.setCreateTime(now);
            insert.setUpdateBy(userId);
            insert.setUpdateTime(now);
            insert.setRemark("地灾自动模式配置");
            sysConfigMapper.insert(insert);
            return;
        }
        DzSysConfig update = new DzSysConfig();
        update.setConfigId(current.getConfigId());
        update.setConfigName(name);
        update.setConfigValue(value);
        update.setUpdateBy(userId);
        update.setUpdateTime(now);
        sysConfigMapper.updateById(update);
    }

    private Long resolveCurrentUserId() {
        try {
            return LoginHelper.getUserId();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 判断本次开启是否需要写入新的开启时间。
     *
     * @param enabledBefore 开启前是否已启用
     * @param openedAtBefore 已保存的最近开启时间
     * @return 关闭转开启或缺少开启时间时返回 true
     */
    static boolean shouldWriteOpenedAtOnOpen(Boolean enabledBefore, String openedAtBefore) {
        return !Boolean.TRUE.equals(enabledBefore) || StringUtils.isBlank(openedAtBefore);
    }

    /**
     * 解析配置中的时间字符串。
     *
     * @param value 时间字符串
     * @return 解析成功的时间；为空或非法时返回 null
     */
    static Date parseDate(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return new Date(DateUtil.parse(value).getTime());
        } catch (Exception e) {
            return null;
        }
    }

    private int parseInt(String value, int defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Long parseLong(String value, Long defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
