/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sms;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 任务短信正文生成服务。
 */
public interface ITaskSmsContentService {

    String DEF_RESP_ROLE_PATROL = "patrol";
    String DEF_RESP_ROLE_MONITOR = "monitor";
    String DEF_RESP_ROLE_SPECIAL_MANAGER = "special_manager";
    String DEF_RESP_ROLE_ASSISTANT_MANAGER = "assistant_manager";
    String DEF_RESP_ROLE_RESOURCE_DIRECTOR = "resource_director";

    record DefRespSmsReceiver(String roleType, String receiverName, String receiverPhone) {
    }

    record DefRespSmsContentResult(String roleType, String receiverName, String receiverPhone, String content) {
    }

    record DefRespStartSmsReceiver(String bizKey,
                                   String bizName,
                                   String receiverName,
                                   String receiverPhone,
                                   Integer adRegionLevel,
                                   Integer countyLevel,
                                   List<String> adRegionNames,
                                   List<String> streetNames,
                                   List<String> streetLevelSummaries,
                                   List<String> villageNames,
                                   String smsTemplate) {
    }

    record DefRespStartSmsContentResult(String bizKey,
                                        String bizName,
                                        String receiverName,
                                        String receiverPhone,
                                        Integer adRegionLevel,
                                        Integer countyLevel,
                                        List<String> adRegionNames,
                                        List<String> streetNames,
                                        List<String> streetLevelSummaries,
                                        List<String> villageNames,
                                        String content) {
    }

    /**
     * 为任务生成短信正文并回填到任务对象。
     *
     * @param task 任务
     * @return 生成后的短信正文
     */
    String populateSmsContent(DzTaskDistList task);

    /**
     * 生成单任务短信正文快照，不修改入参（不含推送聚合逻辑）。
     *
     * @param task 任务
     * @return 短信正文
     */
    String generateSmsContent(DzTaskDistList task);

    /**
     * 按任务列表生成推送短信正文。
     *
     * @param tasks 同一发送分组内的任务列表
     * @return 短信正文
     */
    String generatePushSmsContent(List<DzTaskDistList> tasks);

    /**
     * 按任务列表生成聚合短信正文，统一用于推送预览和实际发送。
     *
     * @param tasks 同一发送分组内的任务列表
     * @return 聚合短信正文
     */
    String generateGroupedSmsContent(List<DzTaskDistList> tasks);

    /**
     * 按巡查、监测任务列表生成乡自规所所长监管聚合短信正文。
     *
     * @param tasks 同一所长对应的巡查、监测任务列表
     * @return 所长监管聚合短信正文
     */
    String generatePatrolMonitorDirectorGroupedSmsContent(List<DzTaskDistList> tasks);

    /**
     * 按防御响应任务分组生成聚合短信正文。
     *
     * @param tasks 同一防御响应、同一接收人的任务列表
     * @return 短信正文
     */
    String generateDefRespGroupedSmsContent(List<DzTaskDistList> tasks);

    /**
     * 按防御响应角色生成聚合短信正文。
     *
     * @param defId         防御响应方案ID
     * @param roleType      角色类型
     * @param receiverName  接收人姓名
     * @param receiverPhone 接收人手机号
     * @param referenceDate 参考日期，为空时取当前时间
     * @return 短信正文
     */
    String generateDefRespRoleSmsContent(Long defId, String roleType, String receiverName, String receiverPhone, Date referenceDate);

    /**
     * 按防御响应角色批量生成聚合短信正文。
     *
     * @param defId         防御响应方案ID
     * @param receivers     接收人列表
     * @param referenceDate 参考日期
     * @return 以 roleType|phone 为键的短信正文结果
     */
    Map<String, DefRespSmsContentResult> generateDefRespRoleSmsContents(Long defId, List<DefRespSmsReceiver> receivers, Date referenceDate);

    /**
     * 按启动短信配置批量生成短信正文。
     *
     * @param defId         防御响应方案ID
     * @param receivers     接收人列表
     * @param referenceDate 参考时间
     * @return 以 bizKey|phone 为键的短信正文结果
     */
    Map<String, DefRespStartSmsContentResult> generateDefRespStartSmsContents(Long defId, List<DefRespStartSmsReceiver> receivers, Date referenceDate);
}
