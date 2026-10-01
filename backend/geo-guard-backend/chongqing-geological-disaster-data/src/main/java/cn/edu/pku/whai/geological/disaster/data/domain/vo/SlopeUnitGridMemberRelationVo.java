/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnitGridMemberRelation;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 斜坡单元网格员信息关联视图对象 data_slope_unit_grid_member_relation
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SlopeUnitGridMemberRelation.class)
public class SlopeUnitGridMemberRelationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 斜坡单元ID
     */
    @ExcelProperty(value = "斜坡单元ID")
    private String unitId;

    /**
     * 分管市长
     */
    @ExcelProperty(value = "分管市长")
    private String chargeMayor;

    /**
     * 分管市长电话号码
     */
    @ExcelProperty(value = "分管市长电话号码")
    private String chargeMayorPhone;

    /**
     * 分管负责人
     */
    @ExcelProperty(value = "分管负责人")
    private String chargePerson;

    /**
     * 分管负责人电话号码
     */
    @ExcelProperty(value = "分管负责人电话号码")
    private String chargePersonPhone;

    /**
     * 责任人
     */
    @ExcelProperty(value = "责任人")
    private String responsiblePerson;

    /**
     * 责任人电话号码
     */
    @ExcelProperty(value = "责任人电话号码")
    private String responsiblePersonPhone;

    /**
     * 管理员
     */
    @ExcelProperty(value = "管理员")
    private String adminUser;

    /**
     * 管理员电话号码
     */
    @ExcelProperty(value = "管理员电话号码")
    private String adminUserPhone;

    /**
     * 专管员
     */
    @ExcelProperty(value = "专管员")
    private String specialManager;

    /**
     * 专管员电话号码
     */
    @ExcelProperty(value = "专管员电话号码")
    private String specialManagerPhone;

    /**
     * 协管员
     */
    @ExcelProperty(value = "协管员")
    private String assistantManager;

    /**
     * 协管员电话号码
     */
    @ExcelProperty(value = "协管员电话号码")
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
    @ExcelProperty(value = "监测员")
    private String monitor;

    /**
     * 监测员电话号码
     */
    @ExcelProperty(value = "监测员电话号码")
    private String monitorPhone;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;

    /**
     * 删除标志：0-未删除，1-已删除
     */
    @ExcelProperty(value = "删除标志")
    private Integer isDeleted;
}
