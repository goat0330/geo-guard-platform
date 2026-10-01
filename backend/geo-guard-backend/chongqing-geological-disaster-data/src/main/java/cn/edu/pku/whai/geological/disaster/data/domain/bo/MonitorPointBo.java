/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorPoint;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author lizheng
 * @date 2026-01-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = MonitorPoint.class, reverseConvertGenerate = false)
public class MonitorPointBo extends BaseEntity {
    /**
     * 主键 ID
     */
    private String id;

    /**
     * 监测点名称
     */
    private String monitorName;

    /**
     * 基本信息ID
     */
    private String basicInfoId;

    /**
     * 地理位置
     */
    private String locationDesc;

    /**
     * 灾害类型名称
     */
    private String disasterTypeName;

    /**
     * 灾害类型编码
     */
    private String disasterTypeCode;

    /**
     * 监测点编号
     */
    private String monitorCode;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 填报单位
     */
    private String fillUnitName;

    /**
     * 运维单位
     */
    private String serviceUnitName;

    /**
     * 专业监测预警员
     */
    private String professionalMonitor;

    /**
     * 专业监测员电话
     */
    private String professionalMonitorPhone;

    /**
     * 群测群防员姓名
     */
    private String massPreventionPerson;

    /**
     * 群测群防员电话
     */
    private String massPreventionPhone;

    /**
     * 防治负责人
     */
    private String responsiblePerson;

    /**
     * 防治负责人电话
     */
    private String responsiblePersonPhone;

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
}
