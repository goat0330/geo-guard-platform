/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.data.domain.po.DisasterPreventionPlan;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 防灾预案业务对象 v_disaster_prevention_plan
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DisasterPreventionPlan.class, reverseConvertGenerate = false)
public class DisasterPreventionPlanBo extends BaseEntity {

    /**
     * 主键
     */

    private String id;

    /**
     * 基本情况表id
     */

    private String basicInfoId;

    /**
     * 版本SN
     */

    private String versionSn;

    /**
     * 监测周期
     */

    private String monitoringCycle;

    /**
     * 监测责任人
     */

    private String monitoringResponsiblePerson;

    /**
     * 监测责任人电话
     */

    private String monitoringResponsiblePersonPhone;

    /**
     * 群测群防人
     */

    private String massMonitorPerson;

    /**
     * 群测群防人电话
     */

    private String massMonitorPersonPhone;

    /**
     * 报警方法
     */

    private String alarmMethod;

    /**
     * 报警型号
     */

    private String alarmModel;

    /**
     * 报警人
     */

    private String alarmPerson;

    /**
     * 报警人电话
     */

    private String alarmPersonPhone;

    /**
     * 避灾地点
     */

    private String disasterAvoidanceLocation;

    /**
     * 人员撤离路线
     */

    private String personnelEvacuationRoute;

    /**
     * 防治建议
     */

    private String preventionSuggestions;

    /**
     * 创建人
     */

    private String createdBy;

    /**
     * 创建时间
     */

    private Date createdTime;

    /**
     * 修改人
     */

    private String updatedBy;

    /**
     * 修改时间
     */
    private Date updatedTime;


}
