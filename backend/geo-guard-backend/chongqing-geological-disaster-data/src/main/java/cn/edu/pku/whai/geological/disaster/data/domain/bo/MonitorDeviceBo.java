/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorDevice;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author lizheng
 * @date 2026-01-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = MonitorDevice.class, reverseConvertGenerate = false)
public class MonitorDeviceBo extends BaseEntity {
    /**
     * 设备主键 (sbid)
     */
    private String id;

    /**
     * 设备ID (clientID)
     */
    private String clientId;

    /**
     * 监测点编号
     */
    private String monitorPointCode;

    /**
     * 监测点ID
     */
    private String monitorPointId;

    /**
     * 监测类型 (L1_LF等)
     */
    private String monitorType;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 是否故障 (0-正常, 1-故障)
     */
    private Integer isFault;

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
