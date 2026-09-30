/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessmentWarningRelation;

import java.util.Date;

/**
 * 风险评估预警联动记录 Service。
 */
public interface IDzRiskAssessmentWarningRelationService {

    DzRiskAssessmentWarningRelation getByWarningEventId(Long warningEventId);

    DzRiskAssessmentWarningRelation getByRiskAssessmentId(Long riskAssessmentId);

    DzRiskAssessmentWarningRelation saveOrUpdateRelation(Long warningEventId, Long riskAssessmentId, String slopeUnitId,
                                                         String disposalType, Date disposalTime, Integer validWarning);

    void updateProcessedState(Long relationId, String disposalType, Date disposalTime, Integer validWarning);
}
