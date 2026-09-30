/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sms;

import cn.edu.pku.whai.geological.disaster.service.domain.vo.EvacuationSmsBatchResult;

import java.util.Map;

/**
 * 按 handleId 编排多方案撤离短信群发（生成正文、按范围查号、调用发送预留接口）
 *
 * @author whai
 */
public interface IEvacuationSmsSendService {

    /**
     * 根据方案 id 查询处置与撤离方案列表，遍历每个方案生成一条群发任务并发送（当前发送为预留接口）
     *
     * @param handleId 灾害处置主键
     * @return 多任务汇总结果（总成功/失败/跳过数、各任务明细）
     */
    EvacuationSmsBatchResult sendMassesByHandleId(Long handleId);

    /**
     * 按方案 id 查询方案表，根据灾害发生地区及灾害等级,根据灾害等级给多级负责人发送消息
     *
     * @param handleId 处置与撤离方案主键
     */
    EvacuationSmsBatchResult sendPrincipalByHandleId(Long handleId);

    /**
     * 按方案 id 执行群众群发短信和行政人员群发短信任务
     *
     * @param handleId 处置与撤离方案主键
     */
    Boolean execute(Long handleId);

    /**
     * 按 handleId 生成短信，用于展示
     *
     * @param handleId 处置与撤离方案主键
     */
    Map<String, String> generateSmsByHandleId(Long handleId);


}
