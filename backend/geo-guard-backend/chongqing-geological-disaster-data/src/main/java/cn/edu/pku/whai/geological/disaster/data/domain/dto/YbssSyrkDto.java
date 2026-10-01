/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.dto;

import cn.edu.pku.whai.geological.disaster.data.domain.po.Person;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 一标三实实有人口表 DTO
 * 映射自 ybss_syrk，目标表 data_person
 *
 * @author system
 * @date 2024
 */
@Data
@AutoMapper(target = Person.class, reverseConvertGenerate = false)
public class YbssSyrkDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    @NotBlank(message = "主键ID不能为空")
    private String id;

    @JsonProperty("xm")
    @NotBlank(message = "姓名不能为空")
    private String name;

    @JsonProperty("dh")
    private String phoneNumber;

    @JsonProperty("xb")
    @NotBlank(message = "性别不能为空")
    private String gender;

    @JsonProperty("csrq")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "出生日期不能为空")
    private LocalDate birthday;

    @JsonProperty("zjlx")
    private String idType;

    @JsonProperty("zjhm")
    @NotBlank(message = "证件号码不能为空")
    private String idNumber;

    @JsonProperty("xzdz")
    private String residenceAddress;

    @JsonProperty("hjdz")
    private String householdAddress;

    @JsonProperty("tyywdzbm")
    private String residenceUnifiedSocialCreditCode;

    @JsonProperty("bzdz")
    @NotBlank(message = "居住地统一地址名称不能为空")
    private String residenceUnifiedAddressName;

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
    @NotBlank(message = "乡镇街行政区划代码不能为空")
    private String streetCode;

    @JsonProperty("sqjcwhdm")
    @NotBlank(message = "社区居村委代码不能为空")
    private String communityCode;

    @JsonProperty("sqwgdm")
    private String gridCode;

    @JsonProperty("xqmc")
    private String gridName;

    @JsonProperty("xqdm")
    private String communityAddressCode;

    @JsonProperty("ldh")
    private String buildingNumber;

    @JsonProperty("dyh")
    private String unitNumber;

    @JsonProperty("lch")
    private String floorNumber;

    @JsonProperty("hsh")
    private String houseNumber;

    @JsonProperty("x")
    private BigDecimal longitude;

    @JsonProperty("y")
    private BigDecimal latitude;

    @JsonProperty("cjsj")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonProperty("gxsj")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
