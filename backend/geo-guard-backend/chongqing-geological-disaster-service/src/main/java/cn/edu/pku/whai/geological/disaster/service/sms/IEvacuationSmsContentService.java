/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sms;

import cn.edu.pku.whai.geological.disaster.data.domain.po.EvacuationPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;

/**
 * 撤离短信内容生成服务：根据模板与占位符替换生成紧急通知短信正文
 *
 * @author whai
 */
public interface IEvacuationSmsContentService {

    /**
     * 根据处置任务、撤离方案及可选路线描述，生成短信正文
     *
     * @param handle 灾害处置对象（含斜坡单元、事件类型、响应级别、负责人电话等）
     * @param plan   撤离方案（含撤离区域等）
     * @return 替换占位符后的完整短信内容
     */
    String generate(DzTaskHandle handle, EvacuationPlan plan);
}
