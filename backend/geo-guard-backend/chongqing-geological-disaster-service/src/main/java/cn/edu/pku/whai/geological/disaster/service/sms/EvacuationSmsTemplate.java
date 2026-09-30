/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sms;

/**
 * 地质灾害紧急撤离短信模板常量
 * <p>
 * 占位符说明：
 * <ul>
 *   <li>{{command_region}} - 指挥部行政区</li>
 *   <li>{{event_name}} - 具体事件名称</li>
 *   <li>{{management_unit}} - 行政管理单元</li>
 *   <li>{{evacuation_deadline}} - 撤离时限</li>
 *   <li>{{route_guide}} - 指引路线</li>
 *   <li>{{special_manager_phone}} - 村负责人电话</li>
 *   <li>{{resettlement_address}} - 安置点详细地址</li>
 * </ul>
 *
 * @author whai
 */
public final class EvacuationSmsTemplate {

    private EvacuationSmsTemplate() {
    }

    /**
     * 紧急通知群众短信正文模板（占位符替换后即为最终短信内容）
     */
    public static final String MASSES = """
        {{command_region}}指挥部命令：因{{event_name}}，您所在的{{management_unit}}已启动强制撤离。
        请立即行动：
        撤离时间：务必在30分钟内完成撤离；
        撤离路线：{{route_guide}}；
        携带物品：仅带贵重物品、药品、手机及充电器，不要携带大件行李；
        特殊帮助：老弱病残孕等群体请拨打{{special_manager_phone}}，我们将安排专人协助。
        安置点：{{resettlement_address}}，请遵守安置点秩序，不要随意离开；
        请互相转告，生命安全高于一切！后续将实时通报灾害风险变化，待安全后会通知返程。
                                                            (仅供测试,无实际作用)""";
}
