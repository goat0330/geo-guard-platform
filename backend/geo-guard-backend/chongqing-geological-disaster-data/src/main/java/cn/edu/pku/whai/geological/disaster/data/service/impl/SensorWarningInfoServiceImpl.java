/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;


import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.SensorWarningInfoBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SensorWarningInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SensorWarningInfoVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.SensorWarningInfoMapper;
import cn.edu.pku.whai.geological.disaster.data.service.ISensorWarningInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 传感器预警Service业务层处理
 **/
@RequiredArgsConstructor
@Service
public class SensorWarningInfoServiceImpl implements ISensorWarningInfoService {

    private final SensorWarningInfoMapper baseMapper;

    /**
     * 查询传感器预警
     *
     * @param id 主键
     * @return 传感器预警
     */
    @Override
    public SensorWarningInfoVo queryById(String id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询传感器预警列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 传感器预警分页列表
     */
    @Override
    public TableDataInfo<SensorWarningInfoVo> queryPageList(SensorWarningInfoBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SensorWarningInfo> lqw = buildQueryWrapper(bo);
        Page<SensorWarningInfoVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的传感器预警列表
     *
     * @param bo 查询条件
     * @return 传感器预警列表
     */
    @Override
    public List<SensorWarningInfoVo> queryList(SensorWarningInfoBo bo) {
        LambdaQueryWrapper<SensorWarningInfo> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SensorWarningInfo> buildQueryWrapper(SensorWarningInfoBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SensorWarningInfo> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getSensorId()), SensorWarningInfo::getSensorId, bo.getSensorId());
        lqw.eq(StringUtils.isNotBlank(bo.getMonitoringPointWarningId()), SensorWarningInfo::getMonitoringPointWarningId, bo.getMonitoringPointWarningId());
        lqw.eq(StringUtils.isNotBlank(bo.getDeviceId()), SensorWarningInfo::getDeviceId, bo.getDeviceId());
        lqw.eq(StringUtils.isNotBlank(bo.getMonitoringPointId()), SensorWarningInfo::getMonitoringPointId, bo.getMonitoringPointId());
        lqw.eq(bo.getWarningOccurTime() != null, SensorWarningInfo::getWarningOccurTime, bo.getWarningOccurTime());
        lqw.eq(StringUtils.isNotBlank(bo.getWarningLevelCode()), SensorWarningInfo::getWarningLevelCode, bo.getWarningLevelCode());
        lqw.eq(StringUtils.isNotBlank(bo.getWarningDescription()), SensorWarningInfo::getWarningDescription, bo.getWarningDescription());
        return lqw;
    }

    /**
     * 新增传感器预警
     *
     * @param bo 传感器预警
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(SensorWarningInfoBo bo) {
        SensorWarningInfo add = MapstructUtils.convert(bo, SensorWarningInfo.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改传感器预警
     *
     * @param bo 传感器预警
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(SensorWarningInfoBo bo) {
        SensorWarningInfo update = MapstructUtils.convert(bo, SensorWarningInfo.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SensorWarningInfo entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除传感器预警信息
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
