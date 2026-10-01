/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnitGridMemberRelation;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 斜坡单元网格员信息关联业务对象 data_slope_unit_grid_member_relation
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SlopeUnitGridMemberRelation.class, reverseConvertGenerate = false)
public class SlopeUnitGridMemberRelationBo extends BaseEntity {

    /**
     * 主键ID
     */
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 斜坡单元ID
     */
    @NotBlank(message = "斜坡单元ID不能为空", groups = {AddGroup.class, EditGroup.class})
    private String unitId;

    /**
     * 分管市长
     */
    private String chargeMayor;

    /**
     * 分管市长电话号码
     */
    private String chargeMayorPhone;

    /**
     * 分管负责人
     */
    private String chargePerson;

    /**
     * 分管负责人电话号码
     */
    private String chargePersonPhone;

    /**
     * 责任人
     */
    private String responsiblePerson;

    /**
     * 责任人电话号码
     */
    private String responsiblePersonPhone;

    /**
     * 管理员
     */
    private String adminUser;

    /**
     * 管理员电话号码
     */
    private String adminUserPhone;

    /**
     * 专管员
     */
    private String specialManager;

    /**
     * 专管员电话号码
     */
    private String specialManagerPhone;

    /**
     * 协管员
     */
    private String assistantManager;

    /**
     * 协管员电话号码
     */
    private String assistantManagerPhone;

    /**
     * 巡查员
     */
    private String inspector;

    /**
     * 巡查员电话号码
     */
    private String inspectorPhone;

    /**
     * 监测员
     */
    private String monitor;

    /**
     * 监测员电话号码
     */
    private String monitorPhone;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 删除标志：0-未删除，1-已删除
     */
    private Integer isDeleted;
}
