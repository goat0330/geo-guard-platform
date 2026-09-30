/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import cn.edu.pku.whai.geological.disaster.service.domain.req.GenerateEvacuationPlanReq;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.EvacuationRouteResultVo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 撤离方案生成服务接口
 *
 * @author whai
 */
public interface IEvacuationPlanGenerateService {

    void generateOrModifyEvacuationPlanStream(SseEmitter sseEmitter, GenerateEvacuationPlanReq req);

    /**
     * 根据handleId生成疏散路线
     *
     * @param handleId 处置主键
     * @return 疏散路线结果
     */
    EvacuationRouteResultVo generateEvacuationRoute(Long handleId);
}
