/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandleDetailContent;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 处置管理详情内容业务对象 dz_task_handle_detail_content
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzTaskHandleDetailContent.class, reverseConvertGenerate = false)
public class DzTaskHandleDetailContentBo extends BaseEntity {

    /**
     * 主键id
     */
    @NotNull(message = "id不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 业务主键，仅防御响应会商确认可为空
     */
    private Long bizId;

    /**
     * 业务类型：1-dz_task_handle，2-dz_def_resp_plan
     */
    @NotNull(message = "bizType不能为空", groups = {AddGroup.class})
    private Integer bizType;

    /**
     * 内容类型：1-应急调查报告，2-撤离方案，3-防御响应方案，4-初始报告，5-最终报告，6-复盘报告，7-会商确认
     */
    @NotNull(message = "contentType不能为空", groups = {AddGroup.class})
    private Integer contentType;

    /**
     * 内容文本
     */
    private String planContent;

    /**
     * 内容结构化内容
     */
    private String planContentJson;

    /**
     * 专家建议/提示词
     */
    private String suggest;

    /**
     * 主持人Id
     */
    private Long userId;

    /**
     * 主持人姓名
     */
    private String userName;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 删除状态：0-未删除，1-已删除
     */
    private Integer deleted;

    /**
     * 是否最新：0-否，1-是
     */
    private Integer isLatest;

    /**
     * 所属轮次
     */
    private Integer roundNo;

    /**
     * 状态：0-草稿，1-已提交，2-通过
     */
    private Integer status;

    /**
     * 更新时间
     */
    private Date updateDate;
}
