/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.domain.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 获取房间令牌请求实体类
 */
@Data
public class GetRoomTokenReq {
    /**
     * 房间ID
     */
    @JsonProperty("room_id")
    private Integer roomId;

    /**
     * 用户名称
     */
    @JsonProperty("user_name")
    private String userName;
}
