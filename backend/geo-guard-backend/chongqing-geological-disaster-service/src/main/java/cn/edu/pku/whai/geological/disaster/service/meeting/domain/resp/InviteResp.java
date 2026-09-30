/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.domain.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class InviteResp {
    @JsonProperty("arr_user")
    private List<InviteUserInfo> arrUser;

    @Data
    public static class InviteUserInfo {
        @JsonProperty("user_name")
        private String userName;

        @JsonProperty("user_id")
        private Integer userId;

        @JsonProperty("device_id")
        private String deviceId;

        @JsonProperty("device_sn")
        private String deviceSn;

        /**
         * 邀请结果, 0 成功
         */
        @JsonProperty("result")
        private Integer result;

        @JsonProperty("call_sn")
        private String callSn;
    }
}

