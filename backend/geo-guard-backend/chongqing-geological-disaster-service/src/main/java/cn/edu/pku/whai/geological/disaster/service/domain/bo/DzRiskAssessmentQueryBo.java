/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springaicommunity.mcp.annotation.McpToolParam;

import java.util.Date;
import java.util.List;

/**
 * 斜坡单元风险评估查询对象
 *
 * @author kongweiguang
 * @date 2026-01-07
 */
@Data
public class DzRiskAssessmentQueryBo {

    /**
     * 地区
     */
    @McpToolParam(description = "地区")
    private String area;

    /**
     * 风险等级列表（与 dynamicRiskLevel 一致：0：无风险 1：低风险 2：中风险 3：高风险 4：极高风险）
     */
    @McpToolParam(description = "风险等级 (0：无风险 1：低风险 2：中风险 3：高风险 4：极高风险)")
    private List<Integer> riskLevels;

    /**
     * 开始时间
     */
    @McpToolParam(description = "开始时间 传入字符串格式的 yyyy-MM-dd HH:mm:ss 例如 \"2026-01-07 12:12:12\"")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 结束时间
     */
    @McpToolParam(description = "结束时间 传入字符串格式的 yyyy-MM-dd HH:mm:ss 例如 \"2026-01-07 12:12:12\" ")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /**
     * 试点区
     */
    @McpToolParam(description = "试点区")
    private Integer pilotArea1;

    /**
     * 示范区
     */
    @McpToolParam(description = "示范区")
    private Integer pilotArea2;

    @McpToolParam(description = "gtype为图表类型，line(折线图)，bar(条形图)，pie(饼状图)，如果用户输入指定了图表格式则进行提取，如果没有则默认line。")
    private String gtype;

}
