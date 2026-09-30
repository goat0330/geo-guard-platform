/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 防御响应审批状态更新参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DefRespPlanApprovalStatusBo extends BaseEntity {

    @NotNull(message = "id不能为空", groups = {EditGroup.class})
    private Long id;

    @NotNull(message = "approvalStatus不能为空", groups = {EditGroup.class})
    private Integer approvalStatus;
}
