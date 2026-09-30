/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.domain.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class InviteReq {
    /**
     * 默认调度会议
     */
    @JsonProperty("room_token")
    private String roomToken;

    /**
     * 呼叫人员参加调度
     */
    @JsonProperty("arr_user")
    private List<RoomTokenUserInfo> arrUser;


    @Data
    public static class RoomTokenUserInfo {
        @JsonProperty("user_name")
        private String userName;

        @JsonProperty("user_id")
        private Long userId;

        /**
         * 设备号
         */
        @JsonProperty("device_id")
        private String deviceId;
        /**
         * 设备SN
         */
        @JsonProperty("device_sn")
        private Integer deviceSn;

        @JsonProperty("user_phone")
        private String userPhone;

        /**
         * 入会昵称
         */
        @JsonProperty("nickname")
        private String nickname;

        @JsonProperty("slaver_id")
        private Integer slaverId;

        /**
         * 是否打开Mic   1 打开 0 不开
         */
        @JsonProperty("open_mic")
        private Integer openMic;

        /**
         * 是是否打开摄像头  1 打开 0 不开
         */
        @JsonProperty("open_camera")
        private Integer openCamera;
    }
}
