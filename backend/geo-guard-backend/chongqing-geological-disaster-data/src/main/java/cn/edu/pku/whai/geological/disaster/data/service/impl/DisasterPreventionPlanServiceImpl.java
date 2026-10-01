/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.DisasterPreventionPlanBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.DisasterPreventionPlan;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DisasterPreventionPlanVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.DisasterPreventionPlanMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IDisasterPreventionPlanService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 防灾预案Service业务层处理
 **/
@RequiredArgsConstructor
@Service
public class DisasterPreventionPlanServiceImpl implements IDisasterPreventionPlanService {

    private final DisasterPreventionPlanMapper baseMapper;

    /**
     * 查询防灾预案
     *
     * @param id 主键
     * @return 防灾预案
     */
    @Override
    public DisasterPreventionPlanVo queryById(String id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询防灾预案列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 防灾预案分页列表
     */
    @Override
    public TableDataInfo<DisasterPreventionPlanVo> queryPageList(DisasterPreventionPlanBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DisasterPreventionPlan> lqw = buildQueryWrapper(bo);
        Page<DisasterPreventionPlanVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的防灾预案列表
     *
     * @param bo 查询条件
     * @return 防灾预案列表
     */
    @Override
    public List<DisasterPreventionPlanVo> queryList(DisasterPreventionPlanBo bo) {
        LambdaQueryWrapper<DisasterPreventionPlan> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<DisasterPreventionPlan> buildQueryWrapper(DisasterPreventionPlanBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<DisasterPreventionPlan> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getBasicInfoId()), DisasterPreventionPlan::getBasicInfoId, bo.getBasicInfoId());
        lqw.eq(StringUtils.isNotBlank(bo.getVersionSn()), DisasterPreventionPlan::getVersionSn, bo.getVersionSn());
        lqw.eq(StringUtils.isNotBlank(bo.getMonitoringCycle()), DisasterPreventionPlan::getMonitoringCycle, bo.getMonitoringCycle());
        lqw.eq(StringUtils.isNotBlank(bo.getMonitoringResponsiblePerson()), DisasterPreventionPlan::getMonitoringResponsiblePerson, bo.getMonitoringResponsiblePerson());
        lqw.eq(StringUtils.isNotBlank(bo.getMonitoringResponsiblePersonPhone()), DisasterPreventionPlan::getMonitoringResponsiblePersonPhone, bo.getMonitoringResponsiblePersonPhone());
        lqw.eq(StringUtils.isNotBlank(bo.getMassMonitorPerson()), DisasterPreventionPlan::getMassMonitorPerson, bo.getMassMonitorPerson());
        lqw.eq(StringUtils.isNotBlank(bo.getMassMonitorPersonPhone()), DisasterPreventionPlan::getMassMonitorPersonPhone, bo.getMassMonitorPersonPhone());
        lqw.eq(StringUtils.isNotBlank(bo.getAlarmMethod()), DisasterPreventionPlan::getAlarmMethod, bo.getAlarmMethod());
        lqw.eq(StringUtils.isNotBlank(bo.getAlarmModel()), DisasterPreventionPlan::getAlarmModel, bo.getAlarmModel());
        lqw.eq(StringUtils.isNotBlank(bo.getAlarmPerson()), DisasterPreventionPlan::getAlarmPerson, bo.getAlarmPerson());
        lqw.eq(StringUtils.isNotBlank(bo.getAlarmPersonPhone()), DisasterPreventionPlan::getAlarmPersonPhone, bo.getAlarmPersonPhone());
        lqw.eq(StringUtils.isNotBlank(bo.getDisasterAvoidanceLocation()), DisasterPreventionPlan::getDisasterAvoidanceLocation, bo.getDisasterAvoidanceLocation());
        lqw.eq(StringUtils.isNotBlank(bo.getPersonnelEvacuationRoute()), DisasterPreventionPlan::getPersonnelEvacuationRoute, bo.getPersonnelEvacuationRoute());
        lqw.eq(StringUtils.isNotBlank(bo.getPreventionSuggestions()), DisasterPreventionPlan::getPreventionSuggestions, bo.getPreventionSuggestions());
        lqw.eq(StringUtils.isNotBlank(bo.getCreatedBy()), DisasterPreventionPlan::getCreatedBy, bo.getCreatedBy());
        lqw.eq(bo.getCreatedTime() != null, DisasterPreventionPlan::getCreatedTime, bo.getCreatedTime());
        lqw.eq(StringUtils.isNotBlank(bo.getUpdatedBy()), DisasterPreventionPlan::getUpdatedBy, bo.getUpdatedBy());
        lqw.eq(bo.getUpdatedTime() != null, DisasterPreventionPlan::getUpdatedTime, bo.getUpdatedTime());
        return lqw;
    }

    /**
     * 新增防灾预案
     *
     * @param bo 防灾预案
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(DisasterPreventionPlanBo bo) {
        DisasterPreventionPlan add = MapstructUtils.convert(bo, DisasterPreventionPlan.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改防灾预案
     *
     * @param bo 防灾预案
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(DisasterPreventionPlanBo bo) {
        DisasterPreventionPlan update = MapstructUtils.convert(bo, DisasterPreventionPlan.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(DisasterPreventionPlan entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除防灾预案信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (isValid) {
            // TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteByIds(ids) > 0;
    }
}
