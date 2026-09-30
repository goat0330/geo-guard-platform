/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.domain.vo;

import lombok.Data;

import java.util.Map;

@Data
public class MeetingInfoVo {
    /**
     * 会议id
     */
    private Long meetingId;
    /**
     * 主持人id
     */
    private Long initiator;
    /**
     * 主持人名称
     */
    private String initiatorName;
    /**
     * 处置任务id
     */
    private Long handleId;
    /**
     * 处置进度
     */
    private Integer handleProcess;

    /**
     * 参与者信息
     */
    private Map<Long, MeetingParticipantsStatus> participantsMap;

    /**
     * 会议类型 1：处置管理 2：防御响应  3.预测模式
     */
    private Integer meetingType;

    /**
     * 审批类型 1：专家 2：行政
     */
    private Integer approvalType;

    /**
     * 防御响应方案编号（会议开启时预生成，后续落库时复用）
     */
    private String planCode;
}
