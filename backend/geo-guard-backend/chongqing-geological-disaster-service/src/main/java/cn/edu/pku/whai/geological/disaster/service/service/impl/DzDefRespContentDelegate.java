/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.consts.DefRespPlanTemplateContent;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanGenerateBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzProcessProgressBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzDefRespPlanMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzProcessProgressService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleDetailContentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
class DzDefRespContentDelegate {

    private final DzDefRespPlanServiceImpl service;
    private final DzDefRespPlanMapper baseMapper;
    private final IDzTaskHandleDetailContentService dzTaskHandleDetailContentService;
    private final IDzProcessProgressService dzProcessProgressService;

    DzDefRespContentDelegate(DzDefRespPlanServiceImpl service,
                             IDzTaskHandleDetailContentService dzTaskHandleDetailContentService,
                             IDzProcessProgressService dzProcessProgressService) {
        this.service = service;
        this.baseMapper = service.baseMapper;
        this.dzTaskHandleDetailContentService = dzTaskHandleDetailContentService;
        this.dzProcessProgressService = dzProcessProgressService;
    }

    private void validateCurrentUserDefRespPlanAccess(Long handleId, String county, String streets) { service.validateCurrentUserDefRespPlanAccess(handleId, county, streets); }

    private boolean hasProcessProgress(Long defId, Integer status, Integer roundNo) { return service.hasProcessProgress(defId, status, roundNo); }

    private String buildProcessProgressDescription(DefRespPlan defRespPlan, Integer status, Integer actionType, Integer roundNo) { return service.buildProcessProgressDescription(defRespPlan, status, actionType, roundNo); }

    private boolean isCountyRegionPlan(DefRespPlan plan) { return service.isCountyRegionPlan(plan); }

    private LambdaQueryWrapper<DefRespPlan> buildQueryWrapper(DefRespPlanBo bo) { return service.buildQueryWrapper(bo); }

    public String generateLatestPlanContent(DefRespPlanGenerateBo bo) {
        if (bo == null || bo.getDefId() == null) {
            throw new ServiceException("defId不能为空");
        }
        DefRespPlan defRespPlan = getDefRespPlanById(bo.getDefId());
        validateCurrentUserDefRespPlanAccess(defRespPlan.getHandleId(), defRespPlan.getCounty(), defRespPlan.getStreets());
        String content = generatePlanContent(defRespPlan, bo);
        saveDefRespPlanHistory(defRespPlan, content, defRespPlan.getCurrentRoundNo() == null ? 1 : defRespPlan.getCurrentRoundNo());
        return content;
    }

    /**
     * 查询当前用户辖区防御响应范围
     */

    void saveInitialPlanOnCreate(DefRespPlan defRespPlan) {
        if (!isCountyRegionPlan(defRespPlan) || !DefRespPlanStatusEnum.isStarted(defRespPlan.getStatus())) {
            return;
        }
        Integer roundNo = defRespPlan.getCurrentRoundNo() == null ? 1 : defRespPlan.getCurrentRoundNo();
        String content = generatePlanContent(defRespPlan, new DefRespPlanGenerateBo());
        dzTaskHandleDetailContentService.insertInitialReportContent(defRespPlan.getId(), content, roundNo);
        dzTaskHandleDetailContentService.insertInitialAndFinalReportContent(defRespPlan.getId(), content, roundNo);
        saveDefRespPlanHistory(defRespPlan, content, roundNo);
        if (!hasProcessProgress(defRespPlan.getId(), DefRespPlanStatusEnum.STARTED.getCode(), roundNo)) {
            DzProcessProgressBo progressBo = new DzProcessProgressBo();
            progressBo.setDefId(defRespPlan.getId());
            progressBo.setStatus(DefRespPlanStatusEnum.STARTED.getCode());
            progressBo.setCreateDate(defRespPlan.getCreateDate() == null ? new Date() : defRespPlan.getCreateDate());
            progressBo.setRoundNo(roundNo);
            progressBo.setActionType(1);
            progressBo.setDescription(buildProcessProgressDescription(defRespPlan, DefRespPlanStatusEnum.STARTED.getCode(), 1, roundNo));
            dzProcessProgressService.insertByBo(progressBo);
        }
    }

    /**
     * 更新防御响应方案内容
     */

    void saveDefRespPlanHistory(DefRespPlan defRespPlan, String content, Integer roundNo) {
        dzTaskHandleDetailContentService.saveDefRespPlanContent(defRespPlan.getId(), roundNo, content);
    }

    /**
     * 按 id 获取防御响应方案实体
     */
    DefRespPlan getDefRespPlanById(Long defId) {
        DefRespPlanBo defRespPlanBo = new DefRespPlanBo();
        defRespPlanBo.setId(defId);
        LambdaQueryWrapper<DefRespPlan> queryWrapper = buildQueryWrapper(defRespPlanBo);
        DefRespPlan defRespPlan = baseMapper.selectOne(queryWrapper);
        if (defRespPlan == null) {
            log.error("不存在id为{}的防御响应方案", defId);
            throw new ServiceException("防御响应方案不存在");
        }
        return defRespPlan;
    }

    /**
     * 生成方案正文内容
     */
    String generatePlanContent(DefRespPlan defRespPlan, DefRespPlanGenerateBo bo) {
        Date currentDate = new Date();
        DefRespPlanGenerateBo filledBo = buildGenerateBoWithDefaultValues(defRespPlan, bo, currentDate);
        Map<String, String> planParams = buildPlanParamsByType(defRespPlan.getType(), filledBo, currentDate, filledBo.getCreateDate());
        return fillPlanContentByType(defRespPlan.getType(), planParams, filledBo.getLevel());
    }

    /**
     * 填充方案生成参数默认值
     */
    DefRespPlanGenerateBo buildGenerateBoWithDefaultValues(DefRespPlan defRespPlan, DefRespPlanGenerateBo bo, Date currentDate) {
        DefRespPlanGenerateBo filledBo = new DefRespPlanGenerateBo();
        filledBo.setDefId(defRespPlan.getId());
        filledBo.setCity(defaultIfBlank(bo.getCity(), defRespPlan.getCounty()));
        filledBo.setStreets(defaultIfBlank(bo.getStreets(), defRespPlan.getStreets()));
        filledBo.setTriggerCondition(defaultIfBlank(bo.getTriggerCondition(), defRespPlan.getTriggerCondition()));
        filledBo.setResponsibilityUnit(defaultIfBlank(bo.getResponsibilityUnit(), defRespPlan.getResponsibilityUnit()));
        filledBo.setLevel(bo.getLevel() != null ? bo.getLevel() : defRespPlan.getLevel());
        filledBo.setCreateDate(bo.getCreateDate() != null
            ? bo.getCreateDate()
            : (defRespPlan.getCreateDate() != null ? defRespPlan.getCreateDate() : currentDate));
        filledBo.setRedMonitorFrequency(defaultIfBlank(bo.getRedMonitorFrequency(), "每4小时1次"));
        filledBo.setRedPatrolTimes(defaultIfBlank(bo.getRedPatrolTimes(), "6"));
        filledBo.setOrangeMonitorFrequency(defaultIfBlank(bo.getOrangeMonitorFrequency(), "每8小时1次"));
        filledBo.setOrangePatrolTimes(defaultIfBlank(bo.getOrangePatrolTimes(), "3"));
        filledBo.setYellowMonitorFrequency(defaultIfBlank(bo.getYellowMonitorFrequency(), "每12小时1次"));
        filledBo.setYellowPatrolTimes(defaultIfBlank(bo.getYellowPatrolTimes(), "2"));
        filledBo.setBlueMonitorFrequency(defaultIfBlank(bo.getBlueMonitorFrequency(), "每24小时1次"));
        filledBo.setBluePatrolTimes(defaultIfBlank(bo.getBluePatrolTimes(), "1"));
        return filledBo;
    }

    /**
     * 按类型构建方案模板参数
     */
    Map<String, String> buildPlanParamsByType(Integer type, DefRespPlanGenerateBo bo, Date currentDate, Date planCreateDate) {
        Map<String, String> planParams = new HashMap<>();
        planParams.put(DefRespPlanTemplateContent.KEY_DEFENSE_AREA, bo.getCity() + bo.getStreets());
        planParams.put(DefRespPlanTemplateContent.KEY_TRIGGER_CONDITION, bo.getTriggerCondition());
        planParams.put(DefRespPlanTemplateContent.KEY_EXPLAIN_UNIT, bo.getResponsibilityUnit());
        planParams.put(DefRespPlanTemplateContent.KEY_COMPILE_UNIT, bo.getResponsibilityUnit());
        planParams.put(DefRespPlanTemplateContent.KEY_COMPILE_DATE, new SimpleDateFormat("yyyy年MM月dd日").format(planCreateDate));
        if (DefRespPlanTypeEnum.SINGLE.getCode().equals(type)) {
            planParams.put(DefRespPlanTemplateContent.KEY_START_TIME, new SimpleDateFormat("yyyy年MM月dd日HH时mm分").format(planCreateDate));
            planParams.put(DefRespPlanTemplateContent.KEY_SPECIFIC_AREA_NAME, bo.getStreets());
            planParams.put(DefRespPlanTemplateContent.KEY_REVISION_BASIS_AREA, bo.getStreets());
        } else if (DefRespPlanTypeEnum.REGION.getCode().equals(type)) {
            String areaName = bo.getCity() + bo.getStreets();
            planParams.put(DefRespPlanTemplateContent.KEY_START_TIME, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(currentDate));
            planParams.put(DefRespPlanTemplateContent.KEY_SPECIFIC_AREA_NAME, areaName);
            planParams.put(DefRespPlanTemplateContent.KEY_REVISION_BASIS_AREA, areaName);
        } else {
            throw new ServiceException("防御响应方案类型异常");
        }
        putMonitorFrequencyParams(planParams, bo);
        return planParams;
    }

    /**
     * 填充监测巡查频次模板参数
     */
    void putMonitorFrequencyParams(Map<String, String> params, DefRespPlanGenerateBo bo) {
        params.put(DefRespPlanTemplateContent.KEY_RED_MONITOR_FREQUENCY, bo.getRedMonitorFrequency());
        params.put(DefRespPlanTemplateContent.KEY_RED_PATROL_TIMES, bo.getRedPatrolTimes());
        params.put(DefRespPlanTemplateContent.KEY_ORANGE_MONITOR_FREQUENCY, bo.getOrangeMonitorFrequency());
        params.put(DefRespPlanTemplateContent.KEY_ORANGE_PATROL_TIMES, bo.getOrangePatrolTimes());
        params.put(DefRespPlanTemplateContent.KEY_YELLOW_MONITOR_FREQUENCY, bo.getYellowMonitorFrequency());
        params.put(DefRespPlanTemplateContent.KEY_YELLOW_PATROL_TIMES, bo.getYellowPatrolTimes());
        params.put(DefRespPlanTemplateContent.KEY_BLUE_MONITOR_FREQUENCY, bo.getBlueMonitorFrequency());
        params.put(DefRespPlanTemplateContent.KEY_BLUE_PATROL_TIMES, bo.getBluePatrolTimes());
    }

    /**
     * 按类型填充方案模板正文
     */
    String fillPlanContentByType(Integer type, Map<String, String> planParams, Integer level) {
        if (DefRespPlanTypeEnum.REGION.getCode().equals(type)) {
            Set<Integer> activeLevels = level != null ? Set.of(level) : Set.of(1);
            return DefRespPlanTemplateContent.getFilledRegPlanContent(planParams, activeLevels);
        }
        throw new ServiceException("防御响应方案类型异常");
    }

    /**
     * 空值时使用默认值
     */
    String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.isNotBlank(value) ? value : defaultValue;
    }

}
