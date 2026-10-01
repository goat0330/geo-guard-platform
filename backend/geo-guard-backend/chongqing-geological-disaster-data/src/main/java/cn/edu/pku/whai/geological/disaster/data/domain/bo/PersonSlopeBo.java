/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.data.domain.po.PersonSlope;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 人员表与边坡单元表空间关联业务对象 v_person_slope
 *
 * @author system
 * @date 2026-01-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PersonSlope.class, reverseConvertGenerate = false)
public class PersonSlopeBo extends BaseEntity {

    /**
     * 人员ID
     */
    private String personId;

    /**
     * 姓名
     */
    private String name;

    /**
     * 联系电话
     */
    private String phoneNumber;

    /**
     * 性别
     */
    private String gender;

    /**
     * 出生日期
     */
    private LocalDate birthday;

    /**
     * 证件类型
     */
    private String idType;

    /**
     * 证件号码
     */
    private String idNumber;

    /**
     * 现住地址
     */
    private String residenceAddress;

    /**
     * 户籍地址
     */
    private String householdAddress;

    /**
     * 居住地统一社会信用代码
     */
    private String residenceUnifiedSocialCreditCode;

    /**
     * 居住地统一地址名称
     */
    private String residenceUnifiedAddressName;

    /**
     * 户室唯一ID
     */
    private String houseUnitId;

    /**
     * 房屋建筑代码（3010码）
     */
    private String buildingCode;

    /**
     * 人员省行政区划代码
     */
    private String personProvinceCode;

    /**
     * 人员市行政区划代码
     */
    private String personCityCode;

    /**
     * 人员区县行政区划代码
     */
    private String personCountyCode;

    /**
     * 人员乡镇街行政区划代码
     */
    private String personStreetCode;

    /**
     * 人员社区居村委代码
     */
    private String personCommunityCode;

    /**
     * 人员村行政区划代码
     */
    private String personVillageCode;

    /**
     * 社区网格代码
     */
    private String gridCode;

    /**
     * 小区地址名称
     */
    private String gridName;

    /**
     * 小区地址代码
     */
    private String communityAddressCode;

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
     * 户室号
     */
    private String houseNumber;

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
