package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistListRemark;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务备注业务对象 dz_task_dist_list_remark
 *
 * @author kongweiguang
 * @date 2026-04-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzTaskDistListRemark.class, reverseConvertGenerate = false)
public class DzTaskDistListRemarkBo extends BaseEntity {

    /**
     * 任务表id
     */
    @NotNull(message = "任务id不能为空", groups = {AddGroup.class})
    private Long taskId;

    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空", groups = {AddGroup.class})
    private String remark;

}
