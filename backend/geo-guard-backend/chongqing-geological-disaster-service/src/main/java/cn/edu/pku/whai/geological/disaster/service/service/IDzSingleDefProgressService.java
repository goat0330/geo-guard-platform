/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;

import java.util.Date;

/**
 * 单点防御响应进度协同服务。
 */
public interface IDzSingleDefProgressService {

    void recordStarted(DefRespPlan singlePlan, DzTaskHandle taskHandle, Long operatorUserId, Date progressTime);

    void recordPlanGenerated(Long handleId, Date progressTime);

    void recordConsultationApproved(Long handleId, Integer roundNo, Date progressTime);

    void recordAdminApproved(Long handleId, Integer roundNo, Date progressTime);

    void recordTaskPublished(Long handleId, Date progressTime);

    void recordEnded(Long handleId, Date progressTime);
}
