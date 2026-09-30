/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class LiveKitTokenResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String url;
    private String roomName;
    private String token;
}
