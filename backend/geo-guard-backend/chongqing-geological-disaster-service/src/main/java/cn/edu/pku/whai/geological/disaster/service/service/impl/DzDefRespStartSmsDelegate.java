/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.po.AdRegion;
import cn.edu.pku.whai.geological.disaster.data.mapper.AdRegionMapper;
import cn.edu.pku.whai.geological.disaster.data.utils.LevelCodeUtil;
import cn.edu.pku.whai.geological.disaster.service.cache.DefRespStartSmsPreviewRedisCache;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespExecuteStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.RegionScopeTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzUserAdRegionBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzSmsSendStatBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSmsSendBatch;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespStartSmsPreviewVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzDefRespStartSmsConfigVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzSmsSendStatVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.EvacuationSmsResult;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.SmsSendSummaryVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzDefRespStartSmsConfigService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzSmsSendStatService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import cn.edu.pku.whai.geological.disaster.service.service.impl.helper.DefRespStartSmsAggregationKey;
import cn.edu.pku.whai.geological.disaster.service.sms.ITaskSmsContentService;
import cn.edu.pku.whai.geological.disaster.service.sms.SmsScene;
import cn.edu.pku.whai.geological.disaster.service.sms.defresp.DefRespLifecycleSmsContentBuilder;
import cn.edu.pku.whai.geological.disaster.service.sms.defresp.DefRespSmsEventSnapshot;
import cn.edu.pku.whai.geological.disaster.service.sms.defresp.DefRespSmsEventSnapshot.AlarmSnapshot;
import cn.edu.pku.whai.geological.disaster.service.utils.DefRespAlarmLevelUtil;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DzDefRespStartSmsDelegate {

    private static final Logger log = LoggerFactory.getLogger(DzDefRespStartSmsDelegate.class);

    private final DzTaskDistListServiceImpl service;
    private final DzPushSmsDelegate pushSmsDelegate;
    private final DzDefRespPlanMapper dzDefRespPlanMapper;
    private final IDzDefRespStartSmsConfigService dzDefRespStartSmsConfigService;
    private final ITaskSmsContentService taskSmsContentService;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final IDzUserAdRegionService dzUserAdRegionService;
    private final AdRegionMapper adRegionMapper;
    private final IDzSmsSendStatService dzSmsSendStatService;
    private final DefRespLifecycleSmsContentBuilder lifecycleContentBuilder = new DefRespLifecycleSmsContentBuilder();

    DzDefRespStartSmsDelegate(DzTaskDistListServiceImpl service, DzPushSmsDelegate pushSmsDelegate) {
        this(service, pushSmsDelegate, null);
    }

    DzDefRespStartSmsDelegate(DzTaskDistListServiceImpl service, DzPushSmsDelegate pushSmsDelegate,
                              IDzSmsSendStatService dzSmsSendStatService) {
        this.service = service;
        this.pushSmsDelegate = pushSmsDelegate;
        this.dzDefRespPlanMapper = service.dzDefRespPlanMapper;
        this.dzDefRespStartSmsConfigService = service.dzDefRespStartSmsConfigService;
        this.taskSmsContentService = service.taskSmsContentService;
        this.sysRoleMapper = service.sysRoleMapper;
        this.sysUserMapper = service.sysUserMapper;
        this.sysUserRoleMapper = service.sysUserRoleMapper;
        this.dzUserAdRegionService = service.dzUserAdRegionService;
        this.adRegionMapper = service.adRegionMapper;
        this.dzSmsSendStatService = dzSmsSendStatService;
    }


    public List<DefRespStartSmsPreviewVo> previewDefRespStartSms(Long defId) {
        if (defId == null) {
            throw new ServiceException("defId不能为空");
        }
        DefRespPlan plan = dzDefRespPlanMapper.selectById(defId);
        if (plan == null || Integer.valueOf(1).equals(plan.getDeleted())) {
            throw new ServiceException("防御响应方案不存在");
        }
        List<DefRespStartSmsPreviewVo> previewList = loadOrBuildDefRespStartSmsPreview(defId, new Date(), false, true);
        if (previewList.isEmpty()) {
            throw new ServiceException("当前防御响应方案无可预览的启动短信");
        }
        return previewList;
    }

    /**
     * 预览区域防御响应生命周期短信。所有接收人使用同一份完整正文。
     */
    List<DefRespStartSmsPreviewVo> previewDefRespLifecycleSms(DefRespSmsEventSnapshot event) {
        if (event == null) {
            throw new ServiceException("防御响应短信事件不能为空");
        }
        return loadOrBuildDefRespLifecycleSmsPreview(event, false, true);
    }

    /**
     * 发送区域防御响应生命周期短信。
     */
    SmsSendSummaryVo sendDefRespLifecycleSms(DefRespSmsEventSnapshot event, boolean manual) {
        if (event == null) {
            return emptySendSummary();
        }
        List<DefRespStartSmsPreviewVo> previewList = loadOrBuildDefRespLifecycleSmsPreview(event, true, true);
        if (previewList.isEmpty()) {
            return emptySendSummary();
        }
        String smsType = event.smsType() + (manual ? "_manual" : "");
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;
        for (DefRespStartSmsPreviewVo preview : previewList) {
            if (preview == null || StringUtils.isBlank(preview.getPhone()) || StringUtils.isBlank(preview.getSmsContent())) {
                continue;
            }
            String phone = preview.getPhone().trim();
            if (!manual && hasSuccessfulLifecycleSms(event.defId(), smsType, phone)) {
                totalCount++;
                skipCount++;
                continue;
            }
            EvacuationSmsResult result = pushSmsDelegate.sendSms(new DzPushSmsDelegate.SmsSendParams(
                List.of(phone), preview.getSmsContent(), smsType,
                DzTaskDistList.SOURCE_TYPE_DEF_RESP, event.defId(), null, null,
                preview.getNickName(), SmsScene.DEF_RESP_START));
            totalCount += result.getTotalCount();
            successCount += result.getSuccessCount();
            failCount += result.getFailCount();
            skipCount += result.getSkipCount();
        }
        return SmsSendSummaryVo.builder()
                               .totalCount(totalCount)
                               .successCount(successCount)
                               .failCount(failCount)
                               .skipCount(skipCount)
                               .build();
    }

     SmsSendSummaryVo sendDefRespSmsDirect(Long defId, Date referenceDate) {
        if (defId == null) {
            return emptySendSummary();
        }
        DefRespPlan plan = dzDefRespPlanMapper.selectById(defId);
        if (plan == null || Integer.valueOf(1).equals(plan.getDeleted())) {
            return emptySendSummary();
        }
        List<DefRespStartSmsPreviewVo> previewList = loadOrBuildDefRespStartSmsPreview(
            defId,
            referenceDate == null ? new Date() : referenceDate,
            true,
            true
        );
        if (previewList.isEmpty()) {
            return emptySendSummary();
        }
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;
        for (DefRespStartSmsPreviewVo preview : previewList) {
            if (preview == null || StringUtils.isBlank(preview.getPhone()) || StringUtils.isBlank(preview.getSmsContent())) {
                continue;
            }
            EvacuationSmsResult result = pushSmsDelegate.sendSms(new DzPushSmsDelegate.SmsSendParams(List.of(preview.getPhone()),
                preview.getSmsContent(), preview.getBizName(),
                DzTaskDistList.SOURCE_TYPE_DEF_RESP, defId, null,
                plan.getHandleId(), preview.getNickName(), SmsScene.DEF_RESP_START));
            totalCount += result.getTotalCount();
            successCount += result.getSuccessCount();
            failCount += result.getFailCount();
            skipCount += result.getSkipCount();
        }
        return SmsSendSummaryVo.builder()
                               .totalCount(totalCount)
                               .successCount(successCount)
                               .failCount(failCount)
                               .skipCount(skipCount)
                               .build();
    }

    private SmsSendSummaryVo emptySendSummary() {
        return SmsSendSummaryVo.builder().build();
    }

    private List<DefRespStartSmsPreviewVo> loadOrBuildDefRespLifecycleSmsPreview(DefRespSmsEventSnapshot event,
                                                                                  boolean preferCache,
                                                                                  boolean writeCache) {
        if (!validateLifecycleAlarmSnapshots(event)) {
            return List.of();
        }
        List<DzDefRespStartSmsConfigVo> configs = dzDefRespStartSmsConfigService.queryEnabledList();
        if (configs == null || configs.isEmpty()) {
            return List.of();
        }
        String configDigest = dzDefRespStartSmsConfigService.buildEnabledConfigDigest();
        if (preferCache) {
            List<DefRespStartSmsPreviewVo> cached = DefRespStartSmsPreviewRedisCache.getLifecyclePreview(event, configDigest);
            if (!cached.isEmpty()) {
                return cached;
            }
        }
        List<DefRespStartSmsPreviewVo> previewList = buildDefRespLifecycleSmsPreview(event, configs);
        if (writeCache && !previewList.isEmpty()) {
            DefRespStartSmsPreviewRedisCache.setLifecyclePreview(event, configDigest, previewList);
        }
        return previewList;
    }

    /**
     * 在读取短信配置、缓存、收件人及发送幂等记录前校验预警类事件的必要快照。
     * 映射失败时 fail-closed，避免任何错误正文或发送批次产生。
     */
    private boolean validateLifecycleAlarmSnapshots(DefRespSmsEventSnapshot event) {
        if (event == null || event.source() != DefRespSmsEventSnapshot.Source.ALARM) {
            return true;
        }
        return switch (event.action()) {
            case START -> validateAlarmSnapshot(event, event.afterAlarm());
            case END -> validateAlarmSnapshot(event, event.beforeAlarm());
            case ADJUST -> validateAlarmSnapshot(event, event.beforeAlarm())
                & validateAlarmSnapshot(event, event.afterAlarm());
        };
    }

    private boolean validateAlarmSnapshot(DefRespSmsEventSnapshot event, AlarmSnapshot alarm) {
        if (alarm == null) {
            log.warn("区域防御响应生命周期短信预警快照校验失败, defId={}, action={}, alarmId={}, sourceType={}, rawLevel={}, reason={}",
                event.defId(), event.action(), null, null, null, "missing_alarm");
            return false;
        }
        Integer sourceType = alarm.sourceType();
        Integer rawLevel = alarm.highestWarningLevel();
        String reason;
        if (sourceType == null) {
            reason = "missing_source_type";
        } else if (!DefRespAlarmLevelUtil.isSupportedAlarmSourceType(sourceType)) {
            reason = "unsupported_source_type";
        } else if (rawLevel == null) {
            reason = "missing_warning_level";
        } else if (rawLevel < 1 || rawLevel > 4) {
            reason = "invalid_warning_level";
        } else {
            Integer expectedLevel = DefRespAlarmLevelUtil.resolveDefenseResponseLevel(sourceType, rawLevel);
            if (expectedLevel == null) {
                reason = sourceType == 2 ? "non_triggering_warning_level" : "invalid_warning_level";
            } else if (!Objects.equals(expectedLevel, alarm.defenseResponseLevel())) {
                reason = "invalid_warning_level";
            } else if (alarm.defenseResponseLevel() < 1 || alarm.defenseResponseLevel() > 4) {
                reason = "invalid_warning_level";
            } else {
                return true;
            }
        }
        log.warn("区域防御响应生命周期短信预警快照校验失败, defId={}, action={}, alarmId={}, sourceType={}, rawLevel={}, reason={}",
            event.defId(), event.action(), alarm.alarmId(), sourceType, rawLevel, reason);
        return false;
    }

    private List<DefRespStartSmsPreviewVo> buildDefRespLifecycleSmsPreview(DefRespSmsEventSnapshot event,
                                                                           List<DzDefRespStartSmsConfigVo> configs) {
        String content = lifecycleContentBuilder.build(event).orElse(null);
        if (StringUtils.isBlank(content)) {
            log.warn("区域防御响应生命周期短信缺少必要快照，跳过生成, defId={}, action={}, source={}, roundNo={}",
                event.defId(), event.action(), event.source(), event.roundNo());
            return List.of();
        }
        List<String> eventStreets = Stream.concat(
                event.beforeTownLevels().keySet().stream(),
                event.afterTownLevels().keySet().stream())
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .distinct()
            .toList();
        if (eventStreets.isEmpty()) {
            return List.of();
        }
        Map<String, SysRole> roleMap = loadSysRoleMap(configs);
        Map<String, Set<Long>> userIdsByRoleKey = loadUserIdsByRoleKey(roleMap);
        Map<Long, SysUser> userMap = loadActiveUserMap(userIdsByRoleKey.values().stream()
            .flatMap(Collection::stream).distinct().toList());
        Map<Long, List<DzUserAdRegionVo>> regionBindingsByUserId = loadUserRegionBindings(userMap.keySet().stream().toList());
        Map<String, AdRegion> adRegionMap = loadAdRegionMap(regionBindingsByUserId);
        if (roleMap.isEmpty() || userIdsByRoleKey.isEmpty() || userMap.isEmpty()
            || regionBindingsByUserId.isEmpty() || adRegionMap.isEmpty()) {
            return List.of();
        }

        LinkedHashMap<String, DefRespStartSmsAggregation> recipientsByPhone = new LinkedHashMap<>();
        for (DzDefRespStartSmsConfigVo config : configs) {
            if (config == null || StringUtils.isBlank(config.getBizKey())) {
                continue;
            }
            for (String roleKey : defaultIfEmpty(config.getRoleKeyList())) {
                for (Long userId : defaultIfEmpty(userIdsByRoleKey.get(roleKey))) {
                    SysUser user = userMap.get(userId);
                    if (user == null || StringUtils.isBlank(user.getPhonenumber())) {
                        continue;
                    }
                    String phone = user.getPhonenumber().trim();
                    for (DzUserAdRegionVo binding : defaultIfEmpty(regionBindingsByUserId.get(userId))) {
                        AdRegion region = binding == null ? null : adRegionMap.get(binding.getAdRegionId());
                        if (!matchesPlanRegion(region, event.county(), eventStreets)) {
                            continue;
                        }
                        DefRespStartSmsAggregation recipient = recipientsByPhone.computeIfAbsent(phone,
                            unused -> new DefRespStartSmsAggregation(
                                config.getBizKey(), config.getBizName(), null, userId,
                                pushSmsDelegate.firstNonBlank(user.getNickName(), user.getUserName()), phone));
                        recipient.accept(region);
                    }
                }
            }
        }
        if (recipientsByPhone.isEmpty()) {
            return List.of();
        }
        List<DefRespStartSmsPreviewVo> result = new ArrayList<>();
        recipientsByPhone.values().forEach(recipient -> {
            DefRespStartSmsPreviewVo vo = new DefRespStartSmsPreviewVo();
            vo.setBizKey(recipient.bizKey);
            vo.setBizName(recipient.bizName);
            vo.setUserId(recipient.userId);
            vo.setNickName(recipient.userName);
            vo.setPhone(recipient.phone);
            vo.setAdRegionLevel(recipient.highestLevel);
            vo.setAdRegionIds(new ArrayList<>(recipient.matchedRegions.keySet()));
            vo.setAdRegionNames(resolveMatchedRegionNames(recipient));
            vo.setStreetNames(eventStreets);
            vo.setSmsContent(content);
            result.add(vo);
        });
        return result;
    }

    private boolean hasSuccessfulLifecycleSms(Long defId, String smsType, String phone) {
        try {
            DzSmsSendStatBo query = new DzSmsSendStatBo();
            query.setBizType(DzSmsSendBatch.BIZ_TYPE_DEF_RESP);
            query.setBizId(defId);
            query.setSmsType(smsType);
            query.setReceiverPhone(phone);
            query.setSendStatus(DzSmsSendBatch.SEND_STATUS_SUCCESS);
            List<DzSmsSendStatVo> records = resolveSmsSendStatService().queryList(query);
            return records != null && !records.isEmpty();
        } catch (Exception exception) {
            log.error("查询区域防御响应生命周期短信幂等记录失败, defId={}, smsType={}, phone={}",
                defId, smsType, phone, exception);
            return false;
        }
    }

    private IDzSmsSendStatService resolveSmsSendStatService() {
        return dzSmsSendStatService != null
            ? dzSmsSendStatService
            : SpringUtils.getBean(IDzSmsSendStatService.class);
    }

    private <T> Collection<T> defaultIfEmpty(Collection<T> values) {
        return values == null ? List.of() : values;
    }

    private List<DefRespStartSmsPreviewVo> loadOrBuildDefRespStartSmsPreview(Long defId,
                                                                             Date referenceDate,
                                                                             boolean preferCache,
                                                                             boolean writeCache) {
        List<DzDefRespStartSmsConfigVo> configs = dzDefRespStartSmsConfigService.queryEnabledList();
        if (configs.isEmpty()) {
            return List.of();
        }
        String configDigest = dzDefRespStartSmsConfigService.buildEnabledConfigDigest();
        if (preferCache) {
            List<DefRespStartSmsPreviewVo> cached = DefRespStartSmsPreviewRedisCache.getPreview(defId, configDigest);
            if (!cached.isEmpty()) {
                return cached;
            }
        }
        List<DefRespStartSmsPreviewVo> previewList = buildDefRespStartSmsPreview(defId, referenceDate, configs);
        if (writeCache && !previewList.isEmpty()) {
            DefRespStartSmsPreviewRedisCache.setPreview(defId, configDigest, previewList);
        }
        return previewList;
    }

    private List<DefRespStartSmsPreviewVo> buildDefRespStartSmsPreview(Long defId,
                                                                       Date referenceDate,
                                                                       List<DzDefRespStartSmsConfigVo> configs) {
        DefRespPlan plan = dzDefRespPlanMapper.selectById(defId);
        if (plan == null || Integer.valueOf(1).equals(plan.getDeleted())) {
            return List.of();
        }
        List<DefRespPlan> townPlans = listActiveTownPlansForStartSms(plan);
        Map<String, Integer> townLevelByStreet = townPlans.stream()
                                                          .filter(town -> StringUtils.isNotBlank(town.getStreets()) && town.getLevel() != null)
                                                          .collect(Collectors.toMap(
                                                              town -> town.getStreets().trim(),
                                                              DefRespPlan::getLevel,
                                                              (left, right) -> left,
                                                              LinkedHashMap::new
                                                          ));
        String countyName = resolvePlanCountyName(plan);
        List<String> streets = DzTaskDistListServiceImpl.splitStreets(plan.getStreets());
        if (StringUtils.isBlank(countyName) || streets.isEmpty()) {
            return List.of();
        }
        Map<String, SysRole> roleMap = loadSysRoleMap(configs);
        if (roleMap.isEmpty()) {
            return List.of();
        }
        Map<String, Set<Long>> userIdsByRoleKey = loadUserIdsByRoleKey(roleMap);
        if (userIdsByRoleKey.isEmpty()) {
            return List.of();
        }
        Map<Long, SysUser> userMap = loadActiveUserMap(userIdsByRoleKey.values().stream().flatMap(Collection::stream).distinct().toList());
        if (userMap.isEmpty()) {
            return List.of();
        }
        Map<Long, List<DzUserAdRegionVo>> regionBindingsByUserId = loadUserRegionBindings(userMap.keySet().stream().toList());
        if (regionBindingsByUserId.isEmpty()) {
            return List.of();
        }
        Map<String, AdRegion> adRegionMap = loadAdRegionMap(regionBindingsByUserId);
        LinkedHashMap<DefRespStartSmsAggregationKey, DefRespStartSmsAggregation> aggregationMap = new LinkedHashMap<>();
        for (DzDefRespStartSmsConfigVo config : configs) {
            if (config == null || StringUtils.isBlank(config.getBizKey()) || StringUtils.isBlank(config.getSmsTemplate())) {
                continue;
            }
            for (String roleKey : defaultIfEmpty(config.getRoleKeyList())) {
                Set<Long> candidateUserIds = userIdsByRoleKey.get(roleKey);
                if (candidateUserIds == null || candidateUserIds.isEmpty()) {
                    continue;
                }
                for (Long userId : candidateUserIds) {
                    SysUser sysUser = userMap.get(userId);
                    if (sysUser == null || StringUtils.isBlank(sysUser.getPhonenumber())) {
                        continue;
                    }
                    List<DzUserAdRegionVo> bindings = regionBindingsByUserId.get(userId);
                    if (bindings == null || bindings.isEmpty()) {
                        continue;
                    }
                    for (DzUserAdRegionVo binding : bindings) {
                        AdRegion region = binding == null ? null : adRegionMap.get(binding.getAdRegionId());
                        if (!matchesPlanRegion(region, countyName, streets)) {
                            continue;
                        }
                        DefRespStartSmsAggregationKey key = new DefRespStartSmsAggregationKey(config.getBizKey(), userId);
                        DefRespStartSmsAggregation aggregation = aggregationMap.computeIfAbsent(key, unused -> new DefRespStartSmsAggregation(
                            config.getBizKey(),
                            config.getBizName(),
                            config.getSmsTemplate(),
                            userId,
                            pushSmsDelegate.firstNonBlank(sysUser.getNickName(), sysUser.getUserName()),
                            sysUser.getPhonenumber().trim()
                        ));
                        aggregation.accept(region);
                        String visibleStreet = resolveVisibleStreetName(region);
                        if (StringUtils.isNotBlank(visibleStreet) && townLevelByStreet.containsKey(visibleStreet)) {
                            aggregation.acceptTownLevel(visibleStreet, townLevelByStreet.get(visibleStreet));
                        }
                    }
                }
            }
        }
        if (aggregationMap.isEmpty()) {
            return List.of();
        }
        Integer countyLevel = resolveCountyLevelForStartSms(plan, townPlans);
        List<ITaskSmsContentService.DefRespStartSmsReceiver> receivers = aggregationMap.values().stream()
                                                                                       .map(aggregation -> new ITaskSmsContentService.DefRespStartSmsReceiver(
                                                                                           aggregation.bizKey,
                                                                                           aggregation.bizName,
                                                                                           aggregation.userName,
                                                                                           aggregation.phone,
                                                                                           aggregation.highestLevel,
                                                                                           countyLevel,
                                                                                           resolveMatchedRegionNames(aggregation),
                                                                                           resolveStreetNames(plan, aggregation),
                                                                                           buildStreetLevelSummaries(aggregation),
                                                                                           resolveVillageNames(aggregation),
                                                                                           aggregation.smsTemplate
                                                                                       ))
                                                                                       .toList();
        Map<String, ITaskSmsContentService.DefRespStartSmsContentResult> contentMap =
            taskSmsContentService.generateDefRespStartSmsContents(defId, receivers, referenceDate);
        List<DefRespStartSmsPreviewVo> result = new ArrayList<>();
        for (DefRespStartSmsAggregation aggregation : aggregationMap.values()) {
            String receiverKey = buildDefRespStartSmsReceiverKey(aggregation.bizKey, aggregation.phone);
            ITaskSmsContentService.DefRespStartSmsContentResult contentResult = contentMap.get(receiverKey);
            if (contentResult == null || StringUtils.isBlank(contentResult.content())) {
                continue;
            }
            DefRespStartSmsPreviewVo vo = new DefRespStartSmsPreviewVo();
            vo.setBizKey(aggregation.bizKey);
            vo.setBizName(aggregation.bizName);
            vo.setUserId(aggregation.userId);
            vo.setNickName(aggregation.userName);
            vo.setPhone(aggregation.phone);
            vo.setAdRegionLevel(aggregation.highestLevel);
            vo.setAdRegionIds(new ArrayList<>(aggregation.matchedRegions.keySet()));
            vo.setAdRegionNames(resolveMatchedRegionNames(aggregation));
            vo.setStreetNames(resolveStreetNames(plan, aggregation));
            vo.setVillageNames(resolveVillageNames(aggregation));
            vo.setSmsContent(contentResult.content());
            result.add(vo);
        }
        return result;
    }

    private List<DefRespPlan> listActiveTownPlansForStartSms(DefRespPlan plan) {
        if (plan == null) {
            return List.of();
        }
        Long countyDefId = Objects.equals(plan.getRegionScopeType(), RegionScopeTypeEnum.COUNTY.getCode()) ? plan.getId() : plan.getParentDefId();
        if (countyDefId == null) {
            return List.of();
        }
        return dzDefRespPlanMapper.selectList(
            Wrappers.<DefRespPlan>lambdaQuery()
                    .eq(DefRespPlan::getParentDefId, countyDefId)
                    .eq(DefRespPlan::getType, DefRespPlanTypeEnum.REGION.getCode())
                    .eq(DefRespPlan::getRegionScopeType, RegionScopeTypeEnum.TOWN.getCode())
                    .eq(DefRespPlan::getDeleted, 0)
                    .in(DefRespPlan::getStatus, 0, 1, 2, 3, 4, 5)
                    .in(DefRespPlan::getExecuteStatus,
                        DefRespExecuteStatusEnum.NOT_STARTED.getCode(),
                        DefRespExecuteStatusEnum.RUNNING.getCode())
        );
    }

    private Integer resolveCountyLevelForStartSms(DefRespPlan plan, List<DefRespPlan> townPlans) {
        if (plan != null && plan.getLevel() != null) {
            return plan.getLevel();
        }
        if (townPlans == null || townPlans.isEmpty()) {
            return null;
        }
        return townPlans.stream()
                        .map(DefRespPlan::getLevel)
                        .filter(Objects::nonNull)
                        .min(Integer::compareTo)
                        .orElse(null);
    }

    private String resolveVisibleStreetName(AdRegion region) {
        if (region == null) {
            return null;
        }
        return pushSmsDelegate.firstNonBlank(region.getStreet(), region.getName());
    }

    private List<String> buildStreetLevelSummaries(DefRespStartSmsAggregation aggregation) {
        if (aggregation == null || aggregation.visibleTownLevels.isEmpty()) {
            return List.of();
        }
        return aggregation.visibleTownLevels.entrySet().stream()
                                            .collect(Collectors.groupingBy(
                                                Map.Entry::getValue,
                                                LinkedHashMap::new,
                                                Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                                            ))
                                            .entrySet()
                                            .stream()
                                            .map(entry -> String.join("、", entry.getValue()) + "地质灾害防御"
                                                + LevelCodeUtil.resolveDefenseResponseRoman(entry.getKey()) + "级响应")
                                            .toList();
    }

    private String resolvePlanCountyName(DefRespPlan plan) {
        return plan == null ? null : pushSmsDelegate.firstNonBlank(plan.getCounty());
    }

    private Map<String, SysRole> loadSysRoleMap(List<DzDefRespStartSmsConfigVo> configs) {
        List<String> roleKeys = configs.stream()
                                       .filter(Objects::nonNull)
                                       .flatMap(config -> defaultIfEmpty(config.getRoleKeyList()).stream())
                                       .distinct()
                                       .toList();
        if (roleKeys.isEmpty()) {
            return Map.of();
        }
        List<SysRole> roles = sysRoleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                                                               .in(SysRole::getRoleKey, roleKeys)
                                                               .eq(SysRole::getDelFlag, "0")
                                                               .eq(SysRole::getStatus, "0"));
        if (roles == null || roles.isEmpty()) {
            return Map.of();
        }
        return roles.stream()
                    .filter(Objects::nonNull)
                    .filter(role -> StringUtils.isNotBlank(role.getRoleKey()) && role.getRoleId() != null)
                    .collect(Collectors.toMap(SysRole::getRoleKey, role -> role, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<String, Set<Long>> loadUserIdsByRoleKey(Map<String, SysRole> roleMap) {
        if (roleMap == null || roleMap.isEmpty()) {
            return Map.of();
        }
        List<Long> roleIds = roleMap.values().stream()
                                    .map(SysRole::getRoleId)
                                    .filter(Objects::nonNull)
                                    .distinct()
                                    .toList();
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                                                                           .in(SysUserRole::getRoleId, roleIds));
        Map<Long, String> roleKeyByRoleId = roleMap.values().stream()
                                                   .collect(Collectors.toMap(SysRole::getRoleId, SysRole::getRoleKey, (left, right) -> left));
        LinkedHashMap<String, Set<Long>> result = new LinkedHashMap<>();
        for (SysUserRole userRole : userRoles) {
            if (userRole == null || userRole.getRoleId() == null || userRole.getUserId() == null) {
                continue;
            }
            String roleKey = roleKeyByRoleId.get(userRole.getRoleId());
            if (StringUtils.isBlank(roleKey)) {
                continue;
            }
            result.computeIfAbsent(roleKey, unused -> new LinkedHashSet<>()).add(userRole.getUserId());
        }
        return result;
    }

    private Map<Long, SysUser> loadActiveUserMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<SysUser> users = sysUserMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                                                               .in(SysUser::getUserId, userIds)
                                                               .eq(SysUser::getDelFlag, "0")
                                                               .eq(SysUser::getStatus, "0"));
        if (users == null || users.isEmpty()) {
            return Map.of();
        }
        return users.stream()
                    .filter(Objects::nonNull)
                    .filter(user -> user.getUserId() != null)
                    .collect(Collectors.toMap(SysUser::getUserId, user -> user, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, List<DzUserAdRegionVo>> loadUserRegionBindings(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        DzUserAdRegionBo bo = new DzUserAdRegionBo();
        bo.setUserIds(userIds);
        List<DzUserAdRegionVo> bindings = dzUserAdRegionService.queryList(bo);
        if (bindings == null || bindings.isEmpty()) {
            return Map.of();
        }
        return bindings.stream()
                       .filter(Objects::nonNull)
                       .filter(binding -> binding.getUserId() != null && StringUtils.isNotBlank(binding.getAdRegionId()))
                       .collect(Collectors.groupingBy(DzUserAdRegionVo::getUserId, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<String, AdRegion> loadAdRegionMap(Map<Long, List<DzUserAdRegionVo>> bindingsByUserId) {
        if (bindingsByUserId == null || bindingsByUserId.isEmpty()) {
            return Map.of();
        }
        List<String> regionIds = bindingsByUserId.values().stream()
                                                 .flatMap(Collection::stream)
                                                 .map(DzUserAdRegionVo::getAdRegionId)
                                                 .filter(StringUtils::isNotBlank)
                                                 .map(String::trim)
                                                 .distinct()
                                                 .toList();
        if (regionIds.isEmpty()) {
            return Map.of();
        }
        List<AdRegion> regions = adRegionMapper.selectList(Wrappers.<AdRegion>lambdaQuery().in(AdRegion::getId, regionIds));
        if (regions == null || regions.isEmpty()) {
            return Map.of();
        }
        return regions.stream()
                      .filter(Objects::nonNull)
                      .filter(region -> StringUtils.isNotBlank(region.getId()))
                      .collect(Collectors.toMap(AdRegion::getId, region -> region, (left, right) -> left, LinkedHashMap::new));
    }

    private boolean matchesPlanRegion(AdRegion region, String countyName, List<String> streets) {
        if (region == null || region.getLevel() == null || StringUtils.isBlank(countyName)) {
            return false;
        }
        String regionCounty = pushSmsDelegate.firstNonBlank(region.getCounty(), region.getName());
        if (!StringUtils.equals(StringUtils.trim(countyName), StringUtils.trim(regionCounty))) {
            return false;
        }
        if (region.getLevel() <= 3) {
            return true;
        }
        String regionStreet = pushSmsDelegate.firstNonBlank(region.getStreet(), region.getName());
        return streets.stream().anyMatch(street -> StringUtils.equals(StringUtils.trim(street), StringUtils.trim(regionStreet)));
    }

    private List<String> resolveMatchedRegionNames(DefRespStartSmsAggregation aggregation) {
        if (aggregation == null || aggregation.matchedRegions.isEmpty()) {
            return List.of();
        }
        return aggregation.matchedRegions.values().stream()
                                         .map(this::resolveDisplayRegionName)
                                         .filter(StringUtils::isNotBlank)
                                         .distinct()
                                         .toList();
    }

    private String resolveDisplayRegionName(AdRegion region) {
        if (region == null) {
            return null;
        }
        if (Objects.equals(region.getLevel(), 5)) {
            return pushSmsDelegate.firstNonBlank(region.getVillage(), region.getCommunity(), region.getName());
        }
        if (Objects.equals(region.getLevel(), 4)) {
            return pushSmsDelegate.firstNonBlank(region.getStreet(), region.getName());
        }
        return pushSmsDelegate.firstNonBlank(region.getCounty(), region.getName());
    }

    private List<String> resolveStreetNames(DefRespPlan plan, DefRespStartSmsAggregation aggregation) {
        if (aggregation == null || aggregation.highestLevel == null) {
            return List.of();
        }
        if (aggregation.highestLevel <= 3) {
            return DzTaskDistListServiceImpl.splitStreets(plan.getStreets());
        }
        if (Objects.equals(aggregation.highestLevel, 4)) {
            return aggregation.matchedRegions.values().stream()
                                             .map(region -> pushSmsDelegate.firstNonBlank(region.getStreet(), region.getName()))
                                             .filter(StringUtils::isNotBlank)
                                             .distinct()
                                             .toList();
        }
        return aggregation.matchedRegions.values().stream()
                                         .map(region -> pushSmsDelegate.firstNonBlank(region.getStreet(), region.getName()))
                                         .filter(StringUtils::isNotBlank)
                                         .distinct()
                                         .toList();
    }

    private List<String> resolveVillageNames(DefRespStartSmsAggregation aggregation) {
        if (aggregation == null || !Objects.equals(aggregation.highestLevel, 5)) {
            return List.of();
        }
        return aggregation.matchedRegions.values().stream()
                                         .map(region -> pushSmsDelegate.firstNonBlank(region.getVillage(), region.getCommunity(), region.getName()))
                                         .filter(StringUtils::isNotBlank)
                                         .distinct()
                                         .toList();
    }

    private String buildDefRespStartSmsReceiverKey(String bizKey, String phone) {
        return service.defaultString(bizKey, "") + "|" + service.defaultString(phone, "");
    }

    private List<String> defaultIfEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }

}
