/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.Person;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 人员信息视图对象
 *
 * @author system
 * @date 2024
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = Person.class)
public class DataPersonVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private String id;

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
     * 省行政区划代码
     */
    private String provinceCode;

    /**
     * 市行政区划代码
     */
    private String cityCode;

    /**
     * 区县行政区划代码
     */
    private String countyCode;

    /**
     * 乡镇街行政区划代码
     */
    private String streetCode;

    /**
     * 社区居村委代码
     */
    private String communityCode;

    /**
     * 村行政区划代码
     */
    private String villageCode;

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
     * 采集时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
