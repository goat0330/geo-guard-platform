/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.domain.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CallResultResp {
    @JsonProperty("arr_result")
    private List<CallResultUserInfo> arrResult;


    @Data
    public static class CallResultUserInfo {
        @JsonProperty("call_sn")
        private String callSn;

        @JsonProperty("status")
        private Integer status;

        @JsonProperty("msg")
        private String msg;
    }
}
