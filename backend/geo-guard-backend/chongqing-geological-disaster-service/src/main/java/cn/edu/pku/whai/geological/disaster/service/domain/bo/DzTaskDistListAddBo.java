package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistListAdd;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 任务上报信息业务对象 dz_task_dist_list_add
 *
 * @author kongweiguang
 * @date 2026-04-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzTaskDistListAdd.class, reverseConvertGenerate = false)
public class DzTaskDistListAddBo extends BaseEntity {

    /**
     * 任务 id
     */
    @NotNull(message = "任务id不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long taskId;

    /**
     * 用户 id
     */
    @NotNull(message = "用户id不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long userId;

    /**
     * 上报内容
     */
    @NotBlank(message = "上报内容不能为空", groups = {AddGroup.class, EditGroup.class})
    private String remark;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createDate;

    /**
     * 上报人id
     */
    private Long reportUserId;
}
