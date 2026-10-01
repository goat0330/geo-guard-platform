/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;


import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.WarningDisposalRecordMonitorBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.WarningDisposalRecordMonitor;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.WarningDisposalRecordMonitorVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.WarningDisposalRecordMonitorMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IWarningDisposalRecordMonitorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 预警处置记录监测Service业务层处理
 **/
@RequiredArgsConstructor
@Service
public class WarningDisposalRecordMonitorServiceImpl implements IWarningDisposalRecordMonitorService {

    private final WarningDisposalRecordMonitorMapper baseMapper;

    /**
     * 查询预警处置记录监测
     *
     * @param id 主键
     * @return 预警处置记录监测
     */
    @Override
    public WarningDisposalRecordMonitorVo queryById(String id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询预警处置记录监测列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 预警处置记录监测分页列表
     */
    @Override
    public TableDataInfo<WarningDisposalRecordMonitorVo> queryPageList(WarningDisposalRecordMonitorBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WarningDisposalRecordMonitor> lqw = buildQueryWrapper(bo);
        Page<WarningDisposalRecordMonitorVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的预警处置记录监测列表
     *
     * @param bo 查询条件
     * @return 预警处置记录监测列表
     */
    @Override
    public List<WarningDisposalRecordMonitorVo> queryList(WarningDisposalRecordMonitorBo bo) {
        LambdaQueryWrapper<WarningDisposalRecordMonitor> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<WarningDisposalRecordMonitor> buildQueryWrapper(WarningDisposalRecordMonitorBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<WarningDisposalRecordMonitor> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getMonitoringPointWarningId()), WarningDisposalRecordMonitor::getMonitoringPointWarningId, bo.getMonitoringPointWarningId());
        lqw.eq(StringUtils.isNotBlank(bo.getWarningLevel()), WarningDisposalRecordMonitor::getWarningLevel, bo.getWarningLevel());
        lqw.eq(bo.getWarningOccurTime() != null, WarningDisposalRecordMonitor::getWarningOccurTime, bo.getWarningOccurTime());
        lqw.eq(StringUtils.isNotBlank(bo.getDisposalTypeCode()), WarningDisposalRecordMonitor::getDisposalTypeCode, bo.getDisposalTypeCode());
        lqw.eq(bo.getBackupWarningPicCount() != null, WarningDisposalRecordMonitor::getBackupWarningPicCount, bo.getBackupWarningPicCount());
        lqw.eq(StringUtils.isNotBlank(bo.getDisposalComments()), WarningDisposalRecordMonitor::getDisposalComments, bo.getDisposalComments());
        lqw.eq(bo.getIsDisposalCompleted() != null, WarningDisposalRecordMonitor::getIsDisposalCompleted, bo.getIsDisposalCompleted());
        lqw.eq(bo.getIsDisposalEffective() != null, WarningDisposalRecordMonitor::getIsDisposalEffective, bo.getIsDisposalEffective());
        lqw.eq(bo.getIsWarningClosed() != null, WarningDisposalRecordMonitor::getIsWarningClosed, bo.getIsWarningClosed());
        lqw.eq(StringUtils.isNotBlank(bo.getDisposalPerson()), WarningDisposalRecordMonitor::getDisposalPerson, bo.getDisposalPerson());
        lqw.eq(bo.getDisposalTime() != null, WarningDisposalRecordMonitor::getDisposalTime, bo.getDisposalTime());
        return lqw;
    }

    /**
     * 新增预警处置记录监测
     *
     * @param bo 预警处置记录监测
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(WarningDisposalRecordMonitorBo bo) {
        WarningDisposalRecordMonitor add = MapstructUtils.convert(bo, WarningDisposalRecordMonitor.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改预警处置记录监测
     *
     * @param bo 预警处置记录监测
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(WarningDisposalRecordMonitorBo bo) {
        WarningDisposalRecordMonitor update = MapstructUtils.convert(bo, WarningDisposalRecordMonitor.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(WarningDisposalRecordMonitor entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除预警处置记录监测信息
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
