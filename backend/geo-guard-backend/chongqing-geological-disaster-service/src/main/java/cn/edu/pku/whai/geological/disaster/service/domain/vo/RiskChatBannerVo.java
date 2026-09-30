/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.util.List;


@Data
public class RiskChatBannerVo {
    /**
     * 斜坡单元数量
     */
    private Long slopUnitCount;
    /**
     * 高风险数量
     */
    private Integer highRiskCount;
    /**
     * 极高风险数量
     */
    private Integer veryHighRiskCount;
    /**
     * 未处理报灾数量
     */
    private Long unHandleReportCount;
    /**
     * 未完成事件总数（事件状态为1、2、3、4）
     */
    private Long unfinishedEventTotalCount;
    /**
     * 处置中任务数量
     */
    private Long handlingTaskCount;
    /**
     * 当前参与中的会议数量
     */
    private Long ongoingMeetingCount;
    /**
     * 重点区域
     */
    private List<RiskChatBannerItemVo> keypointArea;
    /**
     * 任务推送统计
     */
    private TaskStatisticsVo statPush;

    @Data
    public static class RiskChatBannerItemVo {
        private String name;
        private String code;
    }
}
