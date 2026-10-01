/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.req;

import lombok.Data;
import org.springaicommunity.mcp.annotation.McpToolParam;

import java.util.List;

@Data
public class SlopeUnitReq {
    /**
     * 唯一标识符
     */
    @McpToolParam(description = "唯一标识符 例如查询23号 但是传入的时候只填数字23即可", required = false)
    private String id;

    /**
     * 斜坡单元名称
     */
    @McpToolParam(description = "斜坡单元名称", required = false)
    private String name;

    /**
     * 地区范围
     */
    @McpToolParam(description = "所属地区范围 例如恩施市", required = false)
    private String area;

    /**
     * 风险级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)
     */
    @McpToolParam(description = "风险级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)", required = false)
    private List<Integer> dynamicRiskLevelList;

}
