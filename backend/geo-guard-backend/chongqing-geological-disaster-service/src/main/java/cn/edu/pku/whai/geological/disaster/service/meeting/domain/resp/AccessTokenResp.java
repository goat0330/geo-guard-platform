/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.domain.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccessTokenResp {
    public String token;
    @JsonProperty("expires_in")
    public Integer expiresIn;
}
