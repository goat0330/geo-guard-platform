/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.listener;

import cn.edu.pku.whai.geological.disaster.data.event.DataAlarmCreatedEvent;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessChainNodeTextEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.TaskProcessSourceTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainNodeService;
import cn.edu.pku.whai.geological.disaster.service.service.impl.DzTaskDistListServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 预警数据流程链路节点监听器。
 */
@Component
@RequiredArgsConstructor
public class DataAlarmProcessChainNodeListener {

    private final IDzTaskProcessChainNodeService taskProcessChainNodeService;

    @EventListener
    public void onDataAlarmCreated(DataAlarmCreatedEvent event) {
        if (event == null || event.alarmIds().isEmpty()) {
            return;
        }
        for (Long alarmId : event.alarmIds()) {
            if (alarmId == null) {
                continue;
            }
            taskProcessChainNodeService.recordBizNode(
                taskProcessChainNodeService.generateChainId(),
                TaskProcessChainNodeTextEnum.ALARM_REPORT_UPLOAD.getLinkName(),
                TaskProcessChainNodeTextEnum.ALARM_REPORT_UPLOAD.getTriggerReason(),
                TaskProcessBizTypeEnum.ALARM.getCode(),
                alarmId,
                null,
                DzTaskDistListServiceImpl.AUTO_AGENT_USER_ID,
                DzTaskDistListServiceImpl.AUTO_AGENT_NAME,
                TaskProcessSourceTypeEnum.ALARM.getCode()
            );
        }
    }
}
