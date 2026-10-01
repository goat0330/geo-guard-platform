/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;


import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.MonitorTypeConfigBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorTypeConfig;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorTypeConfigVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.MonitorTypeConfigMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IMonitorTypeConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 监测类型配置Service业务层处理
 **/
@RequiredArgsConstructor
@Service
public class MonitorTypeConfigServiceImpl implements IMonitorTypeConfigService {

    private final MonitorTypeConfigMapper baseMapper;

    /**
     * 查询监测类型配置
     *
     * @param id 主键
     * @return 监测类型配置
     */
    @Override
    public MonitorTypeConfigVo queryById(String id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询监测类型配置列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 监测类型配置分页列表
     */
    @Override
    public TableDataInfo<MonitorTypeConfigVo> queryPageList(MonitorTypeConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<MonitorTypeConfig> lqw = buildQueryWrapper(bo);
        Page<MonitorTypeConfigVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的监测类型配置列表
     *
     * @param bo 查询条件
     * @return 监测类型配置列表
     */
    @Override
    public List<MonitorTypeConfigVo> queryList(MonitorTypeConfigBo bo) {
        LambdaQueryWrapper<MonitorTypeConfig> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<MonitorTypeConfig> buildQueryWrapper(MonitorTypeConfigBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<MonitorTypeConfig> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getParentId()), MonitorTypeConfig::getParentId, bo.getParentId());
        lqw.eq(StringUtils.isNotBlank(bo.getMonitoringContent()), MonitorTypeConfig::getMonitoringContent, bo.getMonitoringContent());
        lqw.eq(StringUtils.isNotBlank(bo.getMonitoringMethod()), MonitorTypeConfig::getMonitoringMethod, bo.getMonitoringMethod());
        lqw.eq(StringUtils.isNotBlank(bo.getMonitoringTypeCode()), MonitorTypeConfig::getMonitoringTypeCode, bo.getMonitoringTypeCode());
        lqw.like(StringUtils.isNotBlank(bo.getParameterName()), MonitorTypeConfig::getParameterName, bo.getParameterName());
        lqw.eq(StringUtils.isNotBlank(bo.getParameterType()), MonitorTypeConfig::getParameterType, bo.getParameterType());
        lqw.eq(StringUtils.isNotBlank(bo.getExtendedAttributes()), MonitorTypeConfig::getExtendedAttributes, bo.getExtendedAttributes());
        lqw.eq(bo.getSortNumber() != null, MonitorTypeConfig::getSortNumber, bo.getSortNumber());
        lqw.eq(StringUtils.isNotBlank(bo.getDescription()), MonitorTypeConfig::getDescription, bo.getDescription());
        lqw.eq(StringUtils.isNotBlank(bo.getCreatedBy()), MonitorTypeConfig::getCreatedBy, bo.getCreatedBy());
        lqw.eq(bo.getCreatedTime() != null, MonitorTypeConfig::getCreatedTime, bo.getCreatedTime());
        lqw.eq(StringUtils.isNotBlank(bo.getUpdatedBy()), MonitorTypeConfig::getUpdatedBy, bo.getUpdatedBy());
        lqw.eq(bo.getUpdatedTime() != null, MonitorTypeConfig::getUpdatedTime, bo.getUpdatedTime());
        lqw.eq(StringUtils.isNotBlank(bo.getMonitoringContentCode()), MonitorTypeConfig::getMonitoringContentCode, bo.getMonitoringContentCode());
        lqw.eq(bo.getMonitoringTypeLevel() != null, MonitorTypeConfig::getMonitoringTypeLevel, bo.getMonitoringTypeLevel());
        lqw.eq(StringUtils.isNotBlank(bo.getFieldDescription()), MonitorTypeConfig::getFieldDescription, bo.getFieldDescription());
        lqw.eq(bo.getParameterMaxValue() != null, MonitorTypeConfig::getParameterMaxValue, bo.getParameterMaxValue());
        lqw.eq(bo.getParameterMinValue() != null, MonitorTypeConfig::getParameterMinValue, bo.getParameterMinValue());
        lqw.eq(bo.getWarningThresholdValue() != null, MonitorTypeConfig::getWarningThresholdValue, bo.getWarningThresholdValue());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasurementUnit()), MonitorTypeConfig::getMeasurementUnit, bo.getMeasurementUnit());
        lqw.eq(StringUtils.isNotBlank(bo.getParameterDefaultValue()), MonitorTypeConfig::getParameterDefaultValue, bo.getParameterDefaultValue());
        lqw.eq(StringUtils.isNotBlank(bo.getSupplementaryContent()), MonitorTypeConfig::getSupplementaryContent, bo.getSupplementaryContent());
        lqw.eq(StringUtils.isNotBlank(bo.getFieldLength()), MonitorTypeConfig::getFieldLength, bo.getFieldLength());
        return lqw;
    }

    /**
     * 新增监测类型配置
     *
     * @param bo 监测类型配置
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(MonitorTypeConfigBo bo) {
        MonitorTypeConfig add = MapstructUtils.convert(bo, MonitorTypeConfig.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改监测类型配置
     *
     * @param bo 监测类型配置
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(MonitorTypeConfigBo bo) {
        MonitorTypeConfig update = MapstructUtils.convert(bo, MonitorTypeConfig.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(MonitorTypeConfig entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除监测类型配置信息
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
