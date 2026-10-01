/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.dto;

import cn.edu.pku.whai.geological.disaster.data.domain.po.Organization;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 一标三实实有单位表 DTO
 * 映射自 ybss_sydw，目标表 data_organization
 *
 * @author system
 * @date 2024
 */
@Data
@AutoMapper(target = Organization.class, reverseConvertGenerate = false)
public class YbssSydwDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    @NotBlank(message = "主键ID不能为空")
    private String id;

    @JsonProperty("dwjgmc")
    @NotBlank(message = "单位机构名称不能为空")
    private String organizationName;

    @JsonProperty("tyshxydm")
    @NotBlank(message = "统一社会信用代码不能为空")
    private String unifiedSocialCreditCode;

    @JsonProperty("bzdz")
    @NotBlank(message = "标准地址名称（经营地址）不能为空")
    private String businessAddress;

    @JsonProperty("hxid")
    @NotBlank(message = "户室唯一ID不能为空")
    private String houseUnitId;

    @JsonProperty("fwjzdm")
    @NotBlank(message = "房屋建筑代码不能为空")
    private String buildingCode;

    @JsonProperty("sxzqhdm")
    @NotBlank(message = "省行政区划代码不能为空")
    private String provinceCode;

    @JsonProperty("szxzqhdm")
    @NotBlank(message = "市行政区划代码不能为空")
    private String cityCode;

    @JsonProperty("qxxzqhdm")
    @NotBlank(message = "区县行政区划代码不能为空")
    private String countyCode;

    @JsonProperty("xzjxzqhdm")
    @NotBlank(message = "乡镇街区划代码不能为空")
    private String streetCode;

    @JsonProperty("sqjcwhdm")
    @NotBlank(message = "社区居村委代码不能为空")
    private String communityCode;

    @JsonProperty("sqwgdm")
    private String gridCode;

    @JsonProperty("xqmc")
    private String gridName;

    @JsonProperty("ssjzwmc")
    private String buildingName;

    @JsonProperty("x")
    @NotNull(message = "经度不能为空")
    private BigDecimal longitude;

    @JsonProperty("y")
    @NotNull(message = "纬度不能为空")
    private BigDecimal latitude;

    @JsonProperty("cjsj")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonProperty("gxsj")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
