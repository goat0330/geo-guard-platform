/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.SlopeUnitGridMemberRelationBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnitGridMemberRelation;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGridMemberRelationVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.SlopeUnitGridMemberRelationMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.SlopeUnitMapper;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 斜坡单元网格员信息关联Service业务层处理
 */
@RequiredArgsConstructor
@Service
public class SlopeUnitGridMemberRelationServiceImpl implements ISlopeUnitGridMemberRelationService {

    private static final int NOT_DELETED = 0;
    private static final int DELETED = 1;

    private final SlopeUnitGridMemberRelationMapper baseMapper;
    private final SlopeUnitMapper slopeUnitMapper;

    @Override
    public SlopeUnitGridMemberRelationVo queryById(Long id) {
        return baseMapper.selectVoOne(Wrappers.<SlopeUnitGridMemberRelation>lambdaQuery()
            .eq(SlopeUnitGridMemberRelation::getId, id)
            .eq(SlopeUnitGridMemberRelation::getIsDeleted, NOT_DELETED));
    }

    @Override
    public SlopeUnitGridMemberRelationVo queryByUnitId(String unitId) {
        return baseMapper.selectVoOne(Wrappers.<SlopeUnitGridMemberRelation>lambdaQuery()
            .eq(SlopeUnitGridMemberRelation::getUnitId, unitId)
            .eq(SlopeUnitGridMemberRelation::getIsDeleted, NOT_DELETED)
            .last("limit 1"));
    }

    @Override
    public List<SlopeUnitGridMemberRelationVo> queryByUnitIds(Collection<String> unitIds) {
        if (unitIds == null || unitIds.isEmpty()) {
            return List.of();
        }
        List<String> normalizedUnitIds = unitIds.stream()
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .distinct()
            .toList();
        if (normalizedUnitIds.isEmpty()) {
            return List.of();
        }
        return baseMapper.selectVoList(Wrappers.<SlopeUnitGridMemberRelation>lambdaQuery()
            .in(SlopeUnitGridMemberRelation::getUnitId, normalizedUnitIds)
            .eq(SlopeUnitGridMemberRelation::getIsDeleted, NOT_DELETED)
            .orderByDesc(SlopeUnitGridMemberRelation::getUpdateTime, SlopeUnitGridMemberRelation::getId));
    }

    @Override
    public TableDataInfo<SlopeUnitGridMemberRelationVo> queryPageList(SlopeUnitGridMemberRelationBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SlopeUnitGridMemberRelation> lqw = buildQueryWrapper(bo);
        Page<SlopeUnitGridMemberRelationVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<SlopeUnitGridMemberRelationVo> queryList(SlopeUnitGridMemberRelationBo bo) {
        LambdaQueryWrapper<SlopeUnitGridMemberRelation> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SlopeUnitGridMemberRelation> buildQueryWrapper(SlopeUnitGridMemberRelationBo bo) {
        LambdaQueryWrapper<SlopeUnitGridMemberRelation> lqw = Wrappers.lambdaQuery();
        lqw.eq(SlopeUnitGridMemberRelation::getIsDeleted, NOT_DELETED);
        lqw.eq(bo.getId() != null, SlopeUnitGridMemberRelation::getId, bo.getId());
        lqw.eq(StringUtils.isNotBlank(bo.getUnitId()), SlopeUnitGridMemberRelation::getUnitId, bo.getUnitId());
        lqw.like(StringUtils.isNotBlank(bo.getChargeMayor()), SlopeUnitGridMemberRelation::getChargeMayor, bo.getChargeMayor());
        lqw.like(StringUtils.isNotBlank(bo.getChargeMayorPhone()), SlopeUnitGridMemberRelation::getChargeMayorPhone, bo.getChargeMayorPhone());
        lqw.like(StringUtils.isNotBlank(bo.getChargePerson()), SlopeUnitGridMemberRelation::getChargePerson, bo.getChargePerson());
        lqw.like(StringUtils.isNotBlank(bo.getChargePersonPhone()), SlopeUnitGridMemberRelation::getChargePersonPhone, bo.getChargePersonPhone());
        lqw.like(StringUtils.isNotBlank(bo.getResponsiblePerson()), SlopeUnitGridMemberRelation::getResponsiblePerson, bo.getResponsiblePerson());
        lqw.like(StringUtils.isNotBlank(bo.getResponsiblePersonPhone()), SlopeUnitGridMemberRelation::getResponsiblePersonPhone, bo.getResponsiblePersonPhone());
        lqw.like(StringUtils.isNotBlank(bo.getAdminUser()), SlopeUnitGridMemberRelation::getAdminUser, bo.getAdminUser());
        lqw.like(StringUtils.isNotBlank(bo.getAdminUserPhone()), SlopeUnitGridMemberRelation::getAdminUserPhone, bo.getAdminUserPhone());
        lqw.like(StringUtils.isNotBlank(bo.getSpecialManager()), SlopeUnitGridMemberRelation::getSpecialManager, bo.getSpecialManager());
        lqw.like(StringUtils.isNotBlank(bo.getSpecialManagerPhone()), SlopeUnitGridMemberRelation::getSpecialManagerPhone, bo.getSpecialManagerPhone());
        lqw.like(StringUtils.isNotBlank(bo.getAssistantManager()), SlopeUnitGridMemberRelation::getAssistantManager, bo.getAssistantManager());
        lqw.like(StringUtils.isNotBlank(bo.getAssistantManagerPhone()), SlopeUnitGridMemberRelation::getAssistantManagerPhone, bo.getAssistantManagerPhone());
        lqw.like(StringUtils.isNotBlank(bo.getInspector()), SlopeUnitGridMemberRelation::getInspector, bo.getInspector());
        lqw.like(StringUtils.isNotBlank(bo.getInspectorPhone()), SlopeUnitGridMemberRelation::getInspectorPhone, bo.getInspectorPhone());
        lqw.like(StringUtils.isNotBlank(bo.getMonitor()), SlopeUnitGridMemberRelation::getMonitor, bo.getMonitor());
        lqw.like(StringUtils.isNotBlank(bo.getMonitorPhone()), SlopeUnitGridMemberRelation::getMonitorPhone, bo.getMonitorPhone());
        lqw.orderByDesc(SlopeUnitGridMemberRelation::getUpdateTime, SlopeUnitGridMemberRelation::getId);
        return lqw;
    }

    @Override
    public Boolean insertByBo(SlopeUnitGridMemberRelationBo bo) {
        SlopeUnitGridMemberRelation add = MapstructUtils.convert(bo, SlopeUnitGridMemberRelation.class);
        Date now = new Date();
        add.setCreateTime(add.getCreateTime() == null ? now : add.getCreateTime());
        add.setUpdateTime(now);
        add.setIsDeleted(NOT_DELETED);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(SlopeUnitGridMemberRelationBo bo) {
        SlopeUnitGridMemberRelation update = MapstructUtils.convert(bo, SlopeUnitGridMemberRelation.class);
        SlopeUnitGridMemberRelation exist = baseMapper.selectById(update.getId());
        if (exist == null || Integer.valueOf(DELETED).equals(exist.getIsDeleted())) {
            throw new ServiceException("斜坡单元网格员信息关联不存在");
        }
        update.setUpdateTime(new Date());
        update.setIsDeleted(NOT_DELETED);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    private void validEntityBeforeSave(SlopeUnitGridMemberRelation entity) {
        SlopeUnit slopeUnit = slopeUnitMapper.selectById(entity.getUnitId());
        if (slopeUnit == null) {
            throw new ServiceException("关联的斜坡单元不存在");
        }

        SlopeUnitGridMemberRelation duplicate = baseMapper.selectOne(Wrappers.<SlopeUnitGridMemberRelation>lambdaQuery()
            .eq(SlopeUnitGridMemberRelation::getIsDeleted, NOT_DELETED)
            .eq(SlopeUnitGridMemberRelation::getUnitId, entity.getUnitId())
            .ne(entity.getId() != null, SlopeUnitGridMemberRelation::getId, entity.getId())
            .last("limit 1"));
        if (duplicate != null) {
            throw new ServiceException("当前斜坡单元已存在网格员关联信息");
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        List<SlopeUnitGridMemberRelation> entities = baseMapper.selectByIds(ids);
        if (entities == null || entities.isEmpty()) {
            return false;
        }
        Date now = new Date();
        entities.forEach(entity -> {
            entity.setIsDeleted(DELETED);
            entity.setUpdateTime(now);
        });
        return baseMapper.updateBatchById(entities);
    }
}
