/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import lombok.Data;

import java.util.List;

@Data
public class StartMeetingBo {
    /**
     * 处置任务id
     */
    private Long handleId;
    /**
     * 处置进度
     */
    private Integer handleProcess;

    /**
     * 发起人
     */
    private Long initiator;

    /**
     * 会议参与者
     */
    private List<Long> participants;

    /**
     * 会议类型 1：处置管理 2：防御响应  3.预测模式
     */
    private Integer meetingType;
}
