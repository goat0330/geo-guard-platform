/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.event;

import java.util.List;

/**
 * 预警数据新增事件。
 */
public record DataAlarmCreatedEvent(List<Long> alarmIds) {

    public DataAlarmCreatedEvent {
        alarmIds = alarmIds == null ? List.of() : List.copyOf(alarmIds);
    }
}
