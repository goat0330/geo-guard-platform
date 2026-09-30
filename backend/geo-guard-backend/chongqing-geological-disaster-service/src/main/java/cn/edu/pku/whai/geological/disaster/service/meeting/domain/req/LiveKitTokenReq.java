/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.meeting.domain.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class LiveKitTokenReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "会议ID不能为空")
    private Long meetingId;
}
