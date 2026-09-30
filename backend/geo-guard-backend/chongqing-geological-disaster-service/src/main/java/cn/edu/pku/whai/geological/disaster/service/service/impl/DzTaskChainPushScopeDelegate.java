/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistChainScopePushBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistPushBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskProcessChainFilterBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskChainPushSummaryVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistChainPreviewResultVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistChainPushResultVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistPushVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistSmsPreviewItemVo;
import cn.hutool.core.util.ObjUtil;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyAccessorFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 按链路范围推送适配层。
 */
class DzTaskChainPushScopeDelegate {

    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final String RESULT_PARTIAL_SUCCESS = "PARTIAL_SUCCESS";
    private static final String RESULT_NO_CHAIN_MATCH = "NO_CHAIN_MATCH";
    private static final String RESULT_NO_UNPUSHED_TASK = "NO_UNPUSHED_TASK";
    private static final String RESULT_NO_PUSH_PERMISSION = "NO_PUSH_PERMISSION";

    private final DzTaskDistListServiceImpl service;

    DzTaskChainPushScopeDelegate(DzTaskDistListServiceImpl service) {
        this.service = service;
    }

    Object pushCompatible(TaskDistChainScopePushBo bo) {
        ResolutionContext context = resolveContext(bo);
        if (isNoTaskMatch(context)) {
            return List.of();
        }
        if (context.skipPush()) {
            throw new ServiceException(context.resultMessage());
        }
        return service.push(context.legacyPushBo());
    }

    List<TaskDistSmsPreviewItemVo> previewSmsCompatible(TaskDistChainScopePushBo bo) {
        ResolutionContext context = resolveContext(bo);
        if (isNoTaskMatch(context)) {
            return List.of();
        }
        if (context.skipPush()) {
            throw new ServiceException(context.resultMessage());
        }
        return service.previewSms(context.legacyPushBo());
    }

    TaskDistChainPushResultVo pushByChainScope(TaskDistChainScopePushBo bo) {
        service.validateTaskPushAccess();
        ResolutionContext context = resolveContext(bo);
        TaskDistChainPushResultVo result = new TaskDistChainPushResultVo();
        result.setSummary(context.summary());
        if (context.skipPush()) {
            result.setResultCode(context.resultCode());
            result.setResultMessage(context.resultMessage());
            return result;
        }
        TaskDistPushVo pushResult = service.push(context.legacyPushBo());
        result.setPushResult(pushResult);
        result.setResultCode(context.summary().getDeniedTaskCount() > 0 ? RESULT_PARTIAL_SUCCESS : RESULT_SUCCESS);
        result.setResultMessage(buildSuccessMessage(context.summary(), "推送"));
        return result;
    }

    TaskDistChainPreviewResultVo previewSmsByChainScope(TaskDistChainScopePushBo bo) {
        service.validateTaskPushAccess();
        ResolutionContext context = resolveContext(bo);
        TaskDistChainPreviewResultVo result = new TaskDistChainPreviewResultVo();
        result.setSummary(context.summary());
        if (context.skipPush()) {
            result.setPreviewItems(List.of());
            result.setResultCode(context.resultCode());
            result.setResultMessage(context.resultMessage());
            return result;
        }
        List<TaskDistSmsPreviewItemVo> previewItems = service.previewSms(context.legacyPushBo());
        result.setPreviewItems(previewItems);
        result.setResultCode(context.summary().getDeniedTaskCount() > 0 ? RESULT_PARTIAL_SUCCESS : RESULT_SUCCESS);
        result.setResultMessage(buildSuccessMessage(context.summary(), "预览"));
        return result;
    }

    private ResolutionContext resolveContext(TaskDistChainScopePushBo bo) {
        TaskDistChainScopePushBo normalizedBo = normalizeRequest(bo);
        if (hasExplicitTaskSelectors(normalizedBo)) {
            TaskDistPushBo legacyPushBo = copyToLegacyPushBo(normalizedBo);
            TaskChainPushSummaryVo summary = new TaskChainPushSummaryVo();
            List<Long> explicitTaskIds = legacyPushBo.getTaskIds() == null ? List.of() : legacyPushBo.getTaskIds();
            int explicitCount = explicitTaskIds.isEmpty() && legacyPushBo.getTaskId() != null ? 1 : explicitTaskIds.size();
            summary.setInputChainIdCount(0);
            summary.setFilteredChainIdCount(0);
            summary.setFinalChainIdCount(0);
            summary.setExpandedTaskCount(explicitCount);
            summary.setStatusOneTaskCount(explicitCount);
            summary.setDeduplicatedTaskCount(explicitCount);
            summary.setFinalMatchedTaskCount(explicitCount);
            summary.setDeniedTaskCount(0);
            return new ResolutionContext(summary, legacyPushBo, null, null);
        }
        Set<String> explicitChainIds = normalizedExplicitChainIds(normalizedBo.getChainIds());
        Set<String> filteredChainIds = resolveFilteredChainIds(normalizedBo.getLatestProcessNodeFilter());
        Set<String> finalChainIds = resolveFinalChainIds(explicitChainIds, filteredChainIds);

        TaskChainPushSummaryVo summary = new TaskChainPushSummaryVo();
        summary.setInputChainIdCount(explicitChainIds.size());
        summary.setFilteredChainIdCount(filteredChainIds.size());
        summary.setFinalChainIdCount(finalChainIds.size());
        summary.setExpandedTaskCount(0);
        summary.setStatusOneTaskCount(0);
        summary.setDeduplicatedTaskCount(0);
        summary.setFinalMatchedTaskCount(0);
        summary.setDeniedTaskCount(0);

        if (finalChainIds.isEmpty()) {
            return new ResolutionContext(summary, null, RESULT_NO_CHAIN_MATCH, "未命中任何链路");
        }

        List<Long> expandedTaskIds = service.taskProcessChainNodeService.queryRelatedTaskIdsByChainIds(finalChainIds);
        summary.setExpandedTaskCount(expandedTaskIds.size());
        LinkedHashSet<Long> deduplicatedTaskIds = expandedTaskIds.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        summary.setDeduplicatedTaskCount(deduplicatedTaskIds.size());
        if (deduplicatedTaskIds.isEmpty()) {
            return new ResolutionContext(summary, null, RESULT_NO_UNPUSHED_TASK, "命中链路下未关联可推送任务");
        }

        Map<Long, DzTaskDistList> taskMap = service.baseMapper.selectBatchIds(deduplicatedTaskIds).stream()
            .filter(task -> task != null && task.getId() != null)
            .collect(Collectors.toMap(
                DzTaskDistList::getId,
                task -> task,
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ));
        List<DzTaskDistList> statusOneTasks = deduplicatedTaskIds.stream()
            .map(taskMap::get)
            .filter(Objects::nonNull)
            .filter(task -> Objects.equals(task.getDelete(), 0))
            .filter(task -> Objects.equals(task.getStatus(), DzTaskDistList.STATUS_UNPUSHED))
            .toList();
        summary.setStatusOneTaskCount(statusOneTasks.size());
        if (statusOneTasks.isEmpty()) {
            return new ResolutionContext(summary, null, RESULT_NO_UNPUSHED_TASK, "命中链路下无未推送任务");
        }

        List<Long> authorizedTaskIds = new ArrayList<>();
        int deniedTaskCount = 0;
        for (DzTaskDistList task : statusOneTasks) {
            try {
                service.validatePushTaskAccess(task);
                authorizedTaskIds.add(task.getId());
            } catch (ServiceException ex) {
                deniedTaskCount++;
            }
        }
        summary.setDeniedTaskCount(deniedTaskCount);
        summary.setFinalMatchedTaskCount(authorizedTaskIds.size());
        if (authorizedTaskIds.isEmpty()) {
            return new ResolutionContext(summary, null, RESULT_NO_PUSH_PERMISSION, "命中链路下无当前用户可推送任务");
        }

        return new ResolutionContext(summary, buildLegacyPushBo(authorizedTaskIds), null, null);
    }

    private TaskDistChainScopePushBo normalizeRequest(TaskDistChainScopePushBo bo) {
        if (bo == null) {
            throw new ServiceException("按链路范围推送参数不能为空");
        }
        TaskProcessChainFilterBo mergedFilter = mergeLatestProcessNodeFilter(bo);
        bo.setLatestProcessNodeFilter(mergedFilter);
        boolean hasTaskSelectors = hasExplicitTaskSelectors(bo);
        boolean hasChainIds = ObjUtil.isNotEmpty(bo.getChainIds());
        boolean hasFilter = hasFilterCondition(mergedFilter);
        if (!hasTaskSelectors && !hasChainIds && !hasFilter) {
            throw new ServiceException("taskId、taskIds、chainIds和latestProcessNodeFilter不能同时为空");
        }
        return bo;
    }

    private TaskProcessChainFilterBo mergeLatestProcessNodeFilter(TaskDistChainScopePushBo bo) {
        TaskProcessChainFilterBo mergedFilter = new TaskProcessChainFilterBo();
        copyMeaningfulFilterProperties(bo, mergedFilter);
        copyMeaningfulFilterProperties(bo == null ? null : bo.getLatestProcessNodeFilter(), mergedFilter);
        return hasFilterCondition(mergedFilter) ? mergedFilter : null;
    }

    private Set<String> normalizedExplicitChainIds(Collection<String> chainIds) {
        if (chainIds == null || chainIds.isEmpty()) {
            return Set.of();
        }
        return chainIds.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> resolveFilteredChainIds(TaskProcessChainFilterBo filterBo) {
        if (!hasFilterCondition(filterBo)) {
            return Set.of();
        }
        return service.taskProcessChainSummaryService.queryChainIdsByFilter(filterBo);
    }

    private Set<String> resolveFinalChainIds(Set<String> explicitChainIds, Set<String> filteredChainIds) {
        if (!explicitChainIds.isEmpty() && !filteredChainIds.isEmpty()) {
            LinkedHashSet<String> intersection = new LinkedHashSet<>(explicitChainIds);
            intersection.retainAll(filteredChainIds);
            return intersection;
        }
        if (!explicitChainIds.isEmpty()) {
            return explicitChainIds;
        }
        return filteredChainIds;
    }

    private TaskDistPushBo buildLegacyPushBo(List<Long> taskIds) {
        TaskDistPushBo bo = new TaskDistPushBo();
        bo.setTaskIds(taskIds);
        return bo;
    }

    private boolean isNoTaskMatch(ResolutionContext context) {
        if (context == null || !context.skipPush()) {
            return false;
        }
        return RESULT_NO_CHAIN_MATCH.equals(context.resultCode()) || RESULT_NO_UNPUSHED_TASK.equals(context.resultCode());
    }

    private TaskDistPushBo copyToLegacyPushBo(TaskDistChainScopePushBo bo) {
        TaskDistPushBo legacyPushBo = new TaskDistPushBo();
        BeanUtils.copyProperties(bo, legacyPushBo);
        return legacyPushBo;
    }

    private boolean hasExplicitTaskSelectors(TaskDistChainScopePushBo bo) {
        if (bo == null) {
            return false;
        }
        if (bo.getTaskId() != null) {
            return true;
        }
        return bo.getTaskIds() != null && !bo.getTaskIds().isEmpty();
    }

    private boolean hasFilterCondition(TaskProcessChainFilterBo filterBo) {
        if (filterBo == null) {
            return false;
        }
        for (Field field : TaskProcessChainFilterBo.class.getDeclaredFields()) {
            if (isMeaningfulValue(readFieldValue(filterBo, field))) {
                return true;
            }
        }
        return false;
    }

    private void copyMeaningfulFilterProperties(Object source, TaskProcessChainFilterBo target) {
        if (source == null || target == null) {
            return;
        }
        BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(source);
        for (Field field : TaskProcessChainFilterBo.class.getDeclaredFields()) {
            String fieldName = field.getName();
            if (!beanWrapper.isReadableProperty(fieldName)) {
                continue;
            }
            Object value = beanWrapper.getPropertyValue(fieldName);
            if (!isMeaningfulValue(value)) {
                continue;
            }
            writeFieldValue(target, field, value);
        }
    }

    private Object readFieldValue(TaskProcessChainFilterBo filterBo, Field field) {
        field.setAccessible(true);
        try {
            return field.get(filterBo);
        } catch (IllegalAccessException ex) {
            throw new ServiceException("读取链路筛选条件失败");
        }
    }

    private void writeFieldValue(TaskProcessChainFilterBo target, Field field, Object value) {
        field.setAccessible(true);
        try {
            field.set(target, value);
        } catch (IllegalAccessException ex) {
            throw new ServiceException("写入链路筛选条件失败");
        }
    }

    private boolean isMeaningfulValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String stringValue) {
            return StringUtils.isNotBlank(stringValue);
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        return true;
    }

    private String buildSuccessMessage(TaskChainPushSummaryVo summary, String action) {
        if (summary == null) {
            return action + "成功";
        }
        if (summary.getDeniedTaskCount() != null && summary.getDeniedTaskCount() > 0) {
            return "已" + action + "有权限任务，" + summary.getDeniedTaskCount() + "个任务因权限限制被过滤";
        }
        return action + "成功";
    }

    private record ResolutionContext(TaskChainPushSummaryVo summary, TaskDistPushBo legacyPushBo,
                                     String resultCode, String resultMessage) {

        private boolean skipPush() {
            return legacyPushBo == null;
        }
    }
}
