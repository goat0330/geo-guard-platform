/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;


import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.SensorBasicInfoBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SensorBasicInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SensorBasicInfoVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.SensorBasicInfoMapper;
import cn.edu.pku.whai.geological.disaster.data.service.ISensorBasicInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 传感器基础信息Service业务层处理
 **/
@RequiredArgsConstructor
@Service
public class SensorBasicInfoServiceImpl implements ISensorBasicInfoService {

    private final SensorBasicInfoMapper baseMapper;

    /**
     * 查询传感器基础信息
     *
     * @param id 主键
     * @return 传感器基础信息
     */
    @Override
    public SensorBasicInfoVo queryById(String id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询传感器基础信息列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 传感器基础信息分页列表
     */
    @Override
    public TableDataInfo<SensorBasicInfoVo> queryPageList(SensorBasicInfoBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SensorBasicInfo> lqw = buildQueryWrapper(bo);
        Page<SensorBasicInfoVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的传感器基础信息列表
     *
     * @param bo 查询条件
     * @return 传感器基础信息列表
     */
    @Override
    public List<SensorBasicInfoVo> queryList(SensorBasicInfoBo bo) {
        LambdaQueryWrapper<SensorBasicInfo> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SensorBasicInfo> buildQueryWrapper(SensorBasicInfoBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SensorBasicInfo> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getSensorCode()), SensorBasicInfo::getSensorCode, bo.getSensorCode());
        lqw.eq(StringUtils.isNotBlank(bo.getDeviceId()), SensorBasicInfo::getDeviceId, bo.getDeviceId());
        lqw.eq(StringUtils.isNotBlank(bo.getMonitoringPointId()), SensorBasicInfo::getMonitoringPointId, bo.getMonitoringPointId());
        lqw.eq(StringUtils.isNotBlank(bo.getClientId()), SensorBasicInfo::getClientId, bo.getClientId());
        lqw.eq(StringUtils.isNotBlank(bo.getMonitoringMethodCode()), SensorBasicInfo::getMonitoringMethodCode, bo.getMonitoringMethodCode());
        lqw.eq(StringUtils.isNotBlank(bo.getMonitoringTypeCode()), SensorBasicInfo::getMonitoringTypeCode, bo.getMonitoringTypeCode());
        lqw.like(StringUtils.isNotBlank(bo.getSensorName()), SensorBasicInfo::getSensorName, bo.getSensorName());
        lqw.eq(StringUtils.isNotBlank(bo.getSensorParameterValue()), SensorBasicInfo::getSensorParameterValue, bo.getSensorParameterValue());
        lqw.eq(StringUtils.isNotBlank(bo.getResolution()), SensorBasicInfo::getResolution, bo.getResolution());
        lqw.eq(StringUtils.isNotBlank(bo.getSensitivity()), SensorBasicInfo::getSensitivity, bo.getSensitivity());
        lqw.eq(StringUtils.isNotBlank(bo.getEquipmentParameters()), SensorBasicInfo::getEquipmentParameters, bo.getEquipmentParameters());
        lqw.eq(bo.getStorageDate() != null, SensorBasicInfo::getStorageDate, bo.getStorageDate());
        lqw.eq(StringUtils.isNotBlank(bo.getCommunicationMethod()), SensorBasicInfo::getCommunicationMethod, bo.getCommunicationMethod());
        lqw.eq(StringUtils.isNotBlank(bo.getMonitoringSystemIcon()), SensorBasicInfo::getMonitoringSystemIcon, bo.getMonitoringSystemIcon());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasurementUnit()), SensorBasicInfo::getMeasurementUnit, bo.getMeasurementUnit());
        lqw.eq(bo.getInstallLatitude() != null, SensorBasicInfo::getInstallLatitude, bo.getInstallLatitude());
        lqw.eq(bo.getInstallLongitude() != null, SensorBasicInfo::getInstallLongitude, bo.getInstallLongitude());
        lqw.eq(StringUtils.isNotBlank(bo.getInstallAltitude()), SensorBasicInfo::getInstallAltitude, bo.getInstallAltitude());
        lqw.eq(StringUtils.isNotBlank(bo.getSensorModel()), SensorBasicInfo::getSensorModel, bo.getSensorModel());
        lqw.eq(StringUtils.isNotBlank(bo.getInstallationUnit()), SensorBasicInfo::getInstallationUnit, bo.getInstallationUnit());
        lqw.eq(bo.getInstallationTime() != null, SensorBasicInfo::getInstallationTime, bo.getInstallationTime());
        lqw.eq(StringUtils.isNotBlank(bo.getDataPrecision()), SensorBasicInfo::getDataPrecision, bo.getDataPrecision());
        lqw.eq(StringUtils.isNotBlank(bo.getCollectionFrequency()), SensorBasicInfo::getCollectionFrequency, bo.getCollectionFrequency());
        lqw.eq(StringUtils.isNotBlank(bo.getUploadFrequency()), SensorBasicInfo::getUploadFrequency, bo.getUploadFrequency());
        lqw.eq(StringUtils.isNotBlank(bo.getDeviceReportFrequency()), SensorBasicInfo::getDeviceReportFrequency, bo.getDeviceReportFrequency());
        lqw.eq(StringUtils.isNotBlank(bo.getAlarmReportFrequency()), SensorBasicInfo::getAlarmReportFrequency, bo.getAlarmReportFrequency());
        lqw.eq(StringUtils.isNotBlank(bo.getInstallationLocation()), SensorBasicInfo::getInstallationLocation, bo.getInstallationLocation());
        lqw.eq(StringUtils.isNotBlank(bo.getThresholdValue()), SensorBasicInfo::getThresholdValue, bo.getThresholdValue());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasurementMinValue()), SensorBasicInfo::getMeasurementMinValue, bo.getMeasurementMinValue());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasurementMaxValue()), SensorBasicInfo::getMeasurementMaxValue, bo.getMeasurementMaxValue());
        lqw.eq(StringUtils.isNotBlank(bo.getMonitoringModel()), SensorBasicInfo::getMonitoringModel, bo.getMonitoringModel());
        lqw.eq(bo.getLatestCollectionTime() != null, SensorBasicInfo::getLatestCollectionTime, bo.getLatestCollectionTime());
        lqw.eq(StringUtils.isNotBlank(bo.getCreatedBy()), SensorBasicInfo::getCreatedBy, bo.getCreatedBy());
        lqw.eq(bo.getCreatedTime() != null, SensorBasicInfo::getCreatedTime, bo.getCreatedTime());
        lqw.eq(StringUtils.isNotBlank(bo.getUpdatedBy()), SensorBasicInfo::getUpdatedBy, bo.getUpdatedBy());
        lqw.eq(bo.getUpdatedTime() != null, SensorBasicInfo::getUpdatedTime, bo.getUpdatedTime());
        lqw.eq(StringUtils.isNotBlank(bo.getIsEnabled()), SensorBasicInfo::getIsEnabled, bo.getIsEnabled());
        lqw.eq(bo.getMeasurementStartValue() != null, SensorBasicInfo::getMeasurementStartValue, bo.getMeasurementStartValue());
        lqw.eq(bo.getMeasurementEndValue() != null, SensorBasicInfo::getMeasurementEndValue, bo.getMeasurementEndValue());
        lqw.eq(bo.getIsSyncToScreen() != null, SensorBasicInfo::getIsSyncToScreen, bo.getIsSyncToScreen());
        lqw.eq(StringUtils.isNotBlank(bo.getBatchSyncSensorCode()), SensorBasicInfo::getBatchSyncSensorCode, bo.getBatchSyncSensorCode());
        return lqw;
    }

    /**
     * 新增传感器基础信息
     *
     * @param bo 传感器基础信息
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(SensorBasicInfoBo bo) {
        SensorBasicInfo add = MapstructUtils.convert(bo, SensorBasicInfo.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改传感器基础信息
     *
     * @param bo 传感器基础信息
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(SensorBasicInfoBo bo) {
        SensorBasicInfo update = MapstructUtils.convert(bo, SensorBasicInfo.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SensorBasicInfo entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除传感器基础信息信息
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
