/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springaicommunity.mcp.annotation.McpToolParam;

import java.util.Date;

@Data
public class McpTaskDistListReq {
    @McpToolParam(description = "区域")
    private String area;

    @McpToolParam(description = "开始时间 传入字符串格式的 yyyy-MM-dd HH:mm:ss 例如 \"2026-01-07 12:12:12\"")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @McpToolParam(description = "结束时间 传入字符串格式的 yyyy-MM-dd HH:mm:ss 例如 \"2026-01-07 12:12:12\"")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

}
