/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.data.domain.po.HouseSlope;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 房屋表与边坡单元表空间关联业务对象 v_house_slope
 *
 * @author system
 * @date 2026-01-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = HouseSlope.class, reverseConvertGenerate = false)
public class HouseSlopeBo extends BaseEntity {

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
}
