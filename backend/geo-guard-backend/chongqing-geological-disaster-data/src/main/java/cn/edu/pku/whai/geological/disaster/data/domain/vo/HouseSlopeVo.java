/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.HouseSlope;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房屋表与边坡单元表空间关联视图对象 v_house_slope
 *
 * @author system
 * @date 2026-01-21
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = HouseSlope.class)
public class HouseSlopeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 房屋ID
     */
    private String houseId;

    /**
     * 户室唯一ID
     */
    private String houseUnitId;

    /**
     * 房屋建筑代码（3010码）
     */
    private String buildingCode;

    /**
     * 建筑物名称
     */
    private String buildingName;

    /**
     * 房屋省行政区划代码
     */
    private String houseProvinceCode;

    /**
     * 房屋市行政区划代码
     */
    private String houseCityCode;

    /**
     * 房屋区县行政区划代码
     */
    private String houseCountyCode;

    /**
     * 房屋乡镇街行政区划代码
     */
    private String houseStreetCode;

    /**
     * 房屋社区居委会代码
     */
    private String houseCommunityCode;

    /**
     * 房屋村行政区划代码
     */
    private String houseVillageCode;

    /**
     * 社区网格代码
     */
    private String gridCode;

    /**
     * 街路巷地址代码
     */
    private String streetAddressCode;

    /**
     * 门楼牌号
     */
    private String doorPlateNumber;

    /**
     * 小区地址名称
     */
    private String communityName;

    /**
     * 楼栋号
     */
    private String buildingNumber;

    /**
     * 单元号
     */
    private String unitNumber;

    /**
     * 楼层号
     */
    private String floorNumber;

    /**
     * 房间号
     */
    private String roomNumber;

    /**
     * 经度（坐标x）
     */
    private BigDecimal longitude;

    /**
     * 纬度（坐标y）
     */
    private BigDecimal latitude;

    /**
     * 采集时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 房屋几何对象（PostGIS点）
     */
    private Object houseGeom;

    /**
     * 边坡单元ID
     */
    private String slopeUnitId;

    /**
     * 边坡单元名称
     */
    private String slopeUnitName;

    /**
     * 是否属于588范围
     */
    private Integer pilotArea1;

    /**
     * 是否属于46范围
     */
    private Integer pilotArea2;

    /**
     * 是否属于城区低风险范围
     */
    private Integer urbanArea;

    /**
     * 边坡单元所属省份
     */
    private String slopeUnitProvince;

    /**
     * 边坡单元所属地级市
     */
    private String slopeUnitCity;

    /**
     * 边坡单元所属区/县
     */
    private String slopeUnitCounty;

    /**
     * 边坡单元所属乡镇/街道
     */
    private String slopeUnitStreet;

    /**
     * 边坡单元所属行政村
     */
    private String slopeUnitVillage;

    /**
     * 边坡单元所属社区
     */
    private String slopeUnitCommunity;

    /**
     * 边坡单元中心点坐标
     */
    private String slopeUnitCenter;

    /**
     * 边坡单元面积
     */
    private BigDecimal slopeUnitArea;

    /**
     * 边坡单元省编码
     */
    private String slopeUnitProvinceCode;

    /**
     * 边坡单元市编码
     */
    private String slopeUnitCityCode;

    /**
     * 边坡单元区县编码
     */
    private String slopeUnitCountyCode;

    /**
     * 边坡单元街道/乡镇编码
     */
    private String slopeUnitStreetCode;

    /**
     * 边坡单元村/社区编码
     */
    private String slopeUnitVillageCode;

    /**
     * 边坡单元几何对象（PostGIS多边形）
     */
    private Object slopeUnitGeom;
}
