/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DzDefProcessProgressVo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzProcessProgressBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzProcessProgress;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzProcessProgressVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzProcessProgressMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzProcessProgressService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 防御响应流程进度
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class DzProcessProgressServiceImpl implements IDzProcessProgressService {

    private final DzProcessProgressMapper baseMapper;

    @Override
    public DzProcessProgressVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<DzProcessProgressVo> queryPageList(DzProcessProgressBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DzProcessProgress> lqw = buildQueryWrapper(bo);
        Page<DzProcessProgressVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<DzProcessProgressVo> queryList(DzProcessProgressBo bo) {
        LambdaQueryWrapper<DzProcessProgress> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<DzProcessProgress> buildQueryWrapper(DzProcessProgressBo bo) {
        LambdaQueryWrapper<DzProcessProgress> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getId() != null, DzProcessProgress::getId, bo.getId());
        lqw.eq(bo.getDefId() != null, DzProcessProgress::getDefId, bo.getDefId());
        lqw.eq(bo.getStatus() != null, DzProcessProgress::getStatus, bo.getStatus());
        lqw.eq(bo.getCreateDate() != null, DzProcessProgress::getCreateDate, bo.getCreateDate());
        lqw.eq(bo.getRoundNo() != null, DzProcessProgress::getRoundNo, bo.getRoundNo());
        lqw.eq(bo.getActionType() != null, DzProcessProgress::getActionType, bo.getActionType());
        lqw.orderByDesc(DzProcessProgress::getCreateDate);
        return lqw;
    }

    @Override
    public Boolean insertByBo(DzProcessProgressBo bo) {
        if (bo.getCreateDate() == null) {
            bo.setCreateDate(new Date());
        }
        if (bo.getRoundNo() == null) {
            bo.setRoundNo(1);
        }
        if (bo.getActionType() == null) {
            bo.setActionType(1);
        }
        DzProcessProgress add = MapstructUtils.convert(bo, DzProcessProgress.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(DzProcessProgressBo bo) {
        DzProcessProgress update = MapstructUtils.convert(bo, DzProcessProgress.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    private void validEntityBeforeSave(DzProcessProgress entity) {
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public List<DzDefProcessProgressVo> getDefInfo(Long defId) {
        LambdaQueryWrapper<DzProcessProgress> lqw = new LambdaQueryWrapper<>();
        lqw.eq(DzProcessProgress::getDefId, defId);
        lqw.orderByAsc(DzProcessProgress::getCreateDate)
           .orderByAsc(DzProcessProgress::getStatus);
        List<DzProcessProgress> processProgressList = baseMapper.selectList(lqw);
        List<DzDefProcessProgressVo> dzDefProcessProgressVoList = new ArrayList<>();
        if (processProgressList == null || processProgressList.isEmpty()) {
            return dzDefProcessProgressVoList;
        }
        for (DzProcessProgress processProgress : processProgressList) {
            DzDefProcessProgressVo dzDefProcessProgressVo = new DzDefProcessProgressVo();
            dzDefProcessProgressVo.setDescription(processProgress.getDescription());
            dzDefProcessProgressVo.setStatus(processProgress.getStatus());
            dzDefProcessProgressVo.setCreateDate(processProgress.getCreateDate());
            dzDefProcessProgressVo.setRoundNo(processProgress.getRoundNo());
            dzDefProcessProgressVo.setActionType(processProgress.getActionType());
            dzDefProcessProgressVoList.add(dzDefProcessProgressVo);
        }

        return dzDefProcessProgressVoList;
    }
}
