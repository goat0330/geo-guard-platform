/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.Organization;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 组织机构信息视图对象
 *
 * @author system
 * @date 2024
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = Organization.class)
public class DataOrganizationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private String id;

    /**
     * 单位机构名称
     */
    private String organizationName;

    /**
     * 统一社会信用代码
     */
    private String unifiedSocialCreditCode;

    /**
     * 标准地址名称（经营地址）
     */
    private String businessAddress;

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
     * 乡镇街区划代码
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
     * 所属建筑物名称
     */
    private String buildingName;

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
