/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessmentWarningRelation;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentWarningRelationMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzRiskAssessmentWarningRelationService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 风险评估与第三方预警事件关联 Service 实现。
 */
@Service
@RequiredArgsConstructor
public class DzRiskAssessmentWarningRelationServiceImpl implements IDzRiskAssessmentWarningRelationService {

    private final DzRiskAssessmentWarningRelationMapper baseMapper;

    @Override
    public DzRiskAssessmentWarningRelation getByWarningEventId(Long warningEventId) {
        if (warningEventId == null) {
            return null;
        }
        return baseMapper.selectOne(Wrappers.<DzRiskAssessmentWarningRelation>lambdaQuery()
                                            .eq(DzRiskAssessmentWarningRelation::getWarningEventId, warningEventId)
                                            .last("limit 1"));
    }

    @Override
    public DzRiskAssessmentWarningRelation getByRiskAssessmentId(Long riskAssessmentId) {
        if (riskAssessmentId == null) {
            return null;
        }
        return baseMapper.selectOne(Wrappers.<DzRiskAssessmentWarningRelation>lambdaQuery()
                                            .eq(DzRiskAssessmentWarningRelation::getRiskAssessmentId, riskAssessmentId)
                                            .orderByDesc(DzRiskAssessmentWarningRelation::getUpdateTime)
                                            .orderByDesc(DzRiskAssessmentWarningRelation::getId)
                                            .last("limit 1"));
    }

    @Override
    public DzRiskAssessmentWarningRelation saveOrUpdateRelation(Long warningEventId, Long riskAssessmentId, String slopeUnitId,
                                                                String disposalType, Date disposalTime, Integer validWarning) {
        if (warningEventId == null || riskAssessmentId == null) {
            return null;
        }
        Date now = new Date();
        DzRiskAssessmentWarningRelation relation = getByWarningEventId(warningEventId);
        if (relation == null) {
            relation = new DzRiskAssessmentWarningRelation();
            relation.setWarningEventId(warningEventId);
            relation.setRiskAssessmentId(riskAssessmentId);
            relation.setSlopeUnitId(slopeUnitId);
            // 首次建立关联时不直接回填“最近一次已处理”字段，
            // 交由后续处置变更联动统一更新，避免“新增即已处置”场景被误判为无变化。
            relation.setCreateTime(now);
            relation.setUpdateTime(now);
            baseMapper.insert(relation);
            return relation;
        }
        if (!riskAssessmentId.equals(relation.getRiskAssessmentId()) || !java.util.Objects.equals(slopeUnitId, relation.getSlopeUnitId())) {
            relation.setRiskAssessmentId(riskAssessmentId);
            relation.setSlopeUnitId(slopeUnitId);
            relation.setUpdateTime(now);
            baseMapper.updateById(relation);
        }
        return relation;
    }

    @Override
    public void updateProcessedState(Long relationId, String disposalType, Date disposalTime, Integer validWarning) {
        if (relationId == null) {
            return;
        }
        baseMapper.update(
            null,
            Wrappers.<DzRiskAssessmentWarningRelation>lambdaUpdate()
                    .eq(DzRiskAssessmentWarningRelation::getId, relationId)
                    .set(DzRiskAssessmentWarningRelation::getLastDisposalType, disposalType)
                    .set(DzRiskAssessmentWarningRelation::getLastDisposalTime, disposalTime)
                    .set(DzRiskAssessmentWarningRelation::getLastValidWarning, validWarning)
                    .set(DzRiskAssessmentWarningRelation::getUpdateTime, new Date())
        );
    }
}
