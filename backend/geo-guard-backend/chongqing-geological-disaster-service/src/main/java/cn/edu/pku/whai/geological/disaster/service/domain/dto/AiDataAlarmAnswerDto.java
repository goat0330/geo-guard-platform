/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

/**
 * 大模型解析预警内容后的 JSON 应答结构，用于映射到 DataAlarmBo
 */
@Data
public class AiDataAlarmAnswerDto {

    /**
     * 风险等级
     */
    private Integer level;

    /**
     * 风险标签
     */
    private String city;

    /**
     * 街道，仅用于旧流程展开兼容
     */
    private String streets;

    /**
     * 等级-街道JSON
     */
    @JsonProperty("level_streets_json")
    private String levelStreetsJson;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    @JsonProperty("create_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createDate;

    /**
     * 来源
     */
    private String source;

    /**
     * 原始消息
     */
    private String message;
}
