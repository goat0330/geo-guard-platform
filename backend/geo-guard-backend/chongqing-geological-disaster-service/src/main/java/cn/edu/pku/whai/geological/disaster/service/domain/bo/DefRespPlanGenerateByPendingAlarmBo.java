/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DefRespPlanGenerateByPendingAlarmBo {

    @NotNull(message = "ids不能为空")
    private List<String> ids;

    private String source;
}
