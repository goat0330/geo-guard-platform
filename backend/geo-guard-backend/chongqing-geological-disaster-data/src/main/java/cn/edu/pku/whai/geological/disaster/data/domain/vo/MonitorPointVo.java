/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorPoint;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


/**
 * 监测点基础信息视图对象 v_monitor_point
 **/
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = MonitorPoint.class)
public class MonitorPointVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 监测点唯一主键ID
     */
    @ExcelProperty(value = "监测点唯一主键ID")
    private String id;

    /**
     * 监测点编号
     */
    @ExcelProperty(value = "监测点编号")
    private String monitorCode;

    /**
     * 基本信息关联ID
     */
    @ExcelProperty(value = "基本信息关联ID")
    private String basicInfoId;

    /**
     * 监测点名称
     */
    @ExcelProperty(value = "监测点名称")
    private String monitorName;

    /**
     * 行政区划编码（12位）
     */
    @ExcelProperty(value = "行政区划编码")
    private String administrativeRegionCode;

    /**
     * 地理位置描述
     */
    @ExcelProperty(value = "地理位置描述")
    private String locationDesc;

    /**
     * 海拔（单位：米）
     */
    @ExcelProperty(value = "海拔")

    private Double altitude;

    /**
     * 纬度
     */
    @ExcelProperty(value = "纬度")
    private Double latitude;

    /**
     * 经度
     */
    @ExcelProperty(value = "经度")
    private Double longitude;

    /**
     * 灾害类型编码（如01=滑坡、04=地面塌陷）
     */
    @ExcelProperty(value = "灾害类型编码")
    private String disasterTypeCode;

    /**
     * 建设单位
     */
    @ExcelProperty(value = "建设单位")
    private String constructionUnit;

    /**
     * 责任部门
     */
    @ExcelProperty(value = "责任部门")
    private String responsibleDepartment;

    /**
     * 运维单位
     */
    @ExcelProperty(value = "运维单位")
    private String operationMaintenanceUnit;

    /**
     * 巡查责任人
     */
    @ExcelProperty(value = "巡查责任人")
    private String patrolResponsiblePerson;

    /**
     * 巡查责任人电话
     */
    @ExcelProperty(value = "巡查责任人电话")
    private String patrolResponsiblePersonPhone;

    /**
     * 面巡责任人
     */
    @ExcelProperty(value = "面巡责任人")
    private String areaPatrolResponsiblePerson;

    /**
     * 面巡责任人电话
     */
    @ExcelProperty(value = "面巡责任人电话")
    private String areaPatrolResponsiblePersonPhone;

    /**
     * 专业监测员（监测）
     */
    @ExcelProperty(value = "专业监测员")

    private String professionalMonitor;

    /**
     * 专业监测员电话
     */
    @ExcelProperty(value = "专业监测员电话")
    private String professionalMonitorPhone;

    /**
     * 监测员姓名
     */
    @ExcelProperty(value = "监测员姓名")
    private String massPreventionPerson;

    /**
     * 群测群防员电话
     */
    @ExcelProperty(value = "群测群防员电话")
    private String massPreventionPhone;

    /**
     * 负责人姓名
     */
    @ExcelProperty(value = "负责人姓名")
    private String responsiblePerson;

    /**
     * 负责人电话
     */
    @ExcelProperty(value = "负责人电话")
    private String responsiblePersonPhone;

    /**
     * 巡查监测员
     */
    @ExcelProperty(value = "巡查监测员")
    private String patrolMonitor;

    /**
     * 巡查监测员电话
     */
    @ExcelProperty(value = "巡查监测员电话")
    private String patrolMonitorPhone;

    /**
     * 监测点运维负责人
     */
    @ExcelProperty(value = "监测点运维负责人")
    private String monitoringPointOmPerson;

    /**
     * 监测点运维负责人电话
     */
    @ExcelProperty(value = "监测点运维负责人电话")
    private String monitoringPointOmPersonPhone;

    /**
     * 是否设置预警模型（0=否，1=是）
     */
    @ExcelProperty(value = "是否设置预警模型")

    private Long isWarningModelSet;

    /**
     * 预警频率
     */
    @ExcelProperty(value = "预警频率")
    private Double warningFrequency;

    /**
     * 绑定设备ID（多个用分隔符拼接）
     */
    @ExcelProperty(value = "绑定设备ID")

    private String boundDeviceIds;

    /**
     * 填报人
     */
    @ExcelProperty(value = "填报人")
    private String formFiller;

    /**
     * 填报单位ID
     */
    @ExcelProperty(value = "填报单位ID")
    private String formFillUnitId;

    /**
     * 填报日期
     */
    @ExcelProperty(value = "填报日期")
    private Date formFillDate;

    /**
     * 施工单位
     */
    @ExcelProperty(value = "施工单位")
    private String constructionContractor;

    /**
     * 设备参数
     */
    @ExcelProperty(value = "设备参数")
    private String equipmentParameters;

    /**
     * 是否核销（0=否，1=是）
     */
    @ExcelProperty(value = "是否核销")

    private Long isCanceled;

    /**
     * 年度
     */
    @ExcelProperty(value = "年度")
    private Long year;

    /**
     * 监测点精度（度）
     */
    @ExcelProperty(value = "监测点精度")

    private BigDecimal longitudeDegree;

    /**
     * 监测点精度（分）
     */
    @ExcelProperty(value = "监测点精度")

    private BigDecimal longitudeMinute;

    /**
     * 监测点精度（秒）
     */
    @ExcelProperty(value = "监测点精度")

    private BigDecimal longitudeSecond;

    /**
     * 监测点纬度（度）
     */
    @ExcelProperty(value = "监测点纬度")

    private BigDecimal latitudeDegree;

    /**
     * 监测点纬度（分）
     */
    @ExcelProperty(value = "监测点纬度")

    private BigDecimal latitudeMinute;

    /**
     * 监测点纬度（秒）
     */
    @ExcelProperty(value = "监测点纬度")

    private BigDecimal latitudeSecond;

    /**
     * 国标编号
     */
    @ExcelProperty(value = "国标编号")
    private String nationalStandardCode;

    /**
     * 批量同步推送时间
     */
    @ExcelProperty(value = "批量同步推送时间")
    private Date batchSyncPushTime;

    /**
     * 标签ID（多个用分隔符拼接）
     */
    @ExcelProperty(value = "标签ID")

    private String tagIds;

    /**
     * 管理单位类型
     */
    @ExcelProperty(value = "管理单位类型")
    private String managementUnitType;

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
     * 更新人
     */
    @ExcelProperty(value = "更新人")
    private String updatedBy;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updatedTime;

    /**
     * 斜坡单元ID（su.id AS slope_unit_id）
     */
    private String slopeUnitId;

    /**
     * 斜坡单元名称（su.name AS slope_unit_name）
     */
    private String slopeUnitName;

    /**
     * 试点区域1（su.pilot_area_1）
     */
    private Integer pilotArea1;

    /**
     * 试点区域2（su.pilot_area_2）
     */
    private Integer pilotArea2;

    /**
     * 省名称（su.province）
     */
    private String province;

    /**
     * 市名称（su.city）
     */
    private String city;

    /**
     * 县名称（su.county）
     */
    private String county;

    /**
     * 街道名称（su.street）
     */
    private String street;

    /**
     * 村名称（su.village）
     */
    private String village;

    /**
     * 省编码（su.province_code）
     */
    private String provinceCode;

    /**
     * 市编码（su.city_code）
     */
    private String cityCode;

    /**
     * 县编码（su.county_code）
     */
    private String countyCode;

    /**
     * 街道编码（su.street_code）
     */
    private String streetCode;

    /**
     * 村编码（su.village_code）
     */
    private String villageCode;

    /**
     * 监测点下的设备列表
     */
    private List<MonitorDeviceVo> monitorDeviceList;
}
