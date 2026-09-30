/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.domain.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 查询呼叫结果请求实体类
 */
@Data
public class QueryCallResultReq {
    /**
     * 呼叫序列号列表
     */
    @JsonProperty("call_sn")
    private List<String> callSn;
}
