/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DataAlarmPendingStatusBatchUpdateReq {

    @NotEmpty(message = "ids不能为空")
    private List<String> ids;

    @NotNull(message = "pending_status不能为空")
    @JsonProperty("pending_status")
    private Integer pendingStatus;
}
