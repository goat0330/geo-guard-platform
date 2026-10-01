/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DataAlarmReportAnalysisReq {

    /**
     * 会话ID，映射到 data_alarm.session_id
     */
    private String id;

    /**
     * 消息ID，映射到 data_alarm.code
     */
    @JsonProperty("message_id")
    private String messageId;

    /**
     * 报文类型，例如 report_analysis
     */
    private String type;

    /**
     * 根级关键提示
     */
    private String keyTips;

    /**
     * 报文数据列表
     */
    private List<AlarmItem> data;

    @Data
    public static class AlarmItem {

        /**
         * 预警发布日期，格式 yyyy-MM-dd
         */
        private String time;

        /**
         * 报告发布时间，格式 yyyy-MM-dd HH:mm
         */
        private String publishDate;

        /**
         * 预警发布有效开始时间，格式 yyyy-MM-dd HH:mm
         */
        private String validStartDate;

        /**
         * 预警发布有效结束时间，格式 yyyy-MM-dd HH:mm
         */
        private String validEndDate;

        /**
         * 风险等级分组
         */
        private Map<String, AlarmLevelItem> data;
    }

    @Data
    public static class AlarmLevelItem {

        /**
         * 乡镇名称列表
         */
        private List<String> streets;

    }
}
