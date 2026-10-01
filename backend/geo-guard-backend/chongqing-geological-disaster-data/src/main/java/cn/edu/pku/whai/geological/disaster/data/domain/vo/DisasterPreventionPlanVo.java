/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.DisasterPreventionPlan;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 防灾预案视图对象 v_disaster_prevention_plan
 **/
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DisasterPreventionPlan.class)
public class DisasterPreventionPlanVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private String id;

    /**
     * 基本情况表id
     */
    @ExcelProperty(value = "基本情况表id")
    private String basicInfoId;

    /**
     * 版本SN
     */
    @ExcelProperty(value = "版本SN")
    private String versionSn;

    /**
     * 监测周期
     */
    @ExcelProperty(value = "监测周期")
    private String monitoringCycle;

    /**
     * 监测责任人
     */
    @ExcelProperty(value = "监测责任人")
    private String monitoringResponsiblePerson;

    /**
     * 监测责任人电话
     */
    @ExcelProperty(value = "监测责任人电话")
    private String monitoringResponsiblePersonPhone;

    /**
     * 群测群防人
     */
    @ExcelProperty(value = "群测群防人")
    private String massMonitorPerson;

    /**
     * 群测群防人电话
     */
    @ExcelProperty(value = "群测群防人电话")
    private String massMonitorPersonPhone;

    /**
     * 报警方法
     */
    @ExcelProperty(value = "报警方法")
    private String alarmMethod;

    /**
     * 报警型号
     */
    @ExcelProperty(value = "报警型号")
    private String alarmModel;

    /**
     * 报警人
     */
    @ExcelProperty(value = "报警人")
    private String alarmPerson;

    /**
     * 报警人电话
     */
    @ExcelProperty(value = "报警人电话")
    private String alarmPersonPhone;

    /**
     * 避灾地点
     */
    @ExcelProperty(value = "避灾地点")
    private String disasterAvoidanceLocation;

    /**
     * 人员撤离路线
     */
    @ExcelProperty(value = "人员撤离路线")
    private String personnelEvacuationRoute;

    /**
     * 防治建议
     */
    @ExcelProperty(value = "防治建议")
    private String preventionSuggestions;

    /**
     * 创建人
     */
    @ExcelProperty(value = "创建人")
    private String createdBy;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createdTime;

    /**
     * 修改人
     */
    @ExcelProperty(value = "修改人")
    private String updatedBy;

    /**
     * 修改时间
     */
    @ExcelProperty(value = "修改时间")
    private Date updatedTime;


}
