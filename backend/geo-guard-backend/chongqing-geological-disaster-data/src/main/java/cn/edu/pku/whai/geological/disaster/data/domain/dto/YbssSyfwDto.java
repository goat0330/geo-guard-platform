/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.dto;

import cn.edu.pku.whai.geological.disaster.data.domain.po.House;
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
 * 一标三实实有房屋表 DTO
 * 映射自 ybss_syfw，目标表 data_house
 *
 * @author system
 * @date 2024
 */
@Data
@AutoMapper(target = House.class, reverseConvertGenerate = false)
public class YbssSyfwDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    @NotBlank(message = "主键ID不能为空")
    private String id;

    @JsonProperty("hxid")
    @NotBlank(message = "户室唯一ID不能为空")
    private String houseUnitId;

    @JsonProperty("fwjzdm")
    @NotBlank(message = "房屋建筑代码不能为空")
    private String buildingCode;

    @JsonProperty("bzdz")
    @NotBlank(message = "建筑物名称不能为空")
    private String buildingName;

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
    @NotBlank(message = "社区居委会代码不能为空")
    private String communityCode;

    @JsonProperty("sqwgdm")
    private String gridCode;

    @JsonProperty("jlxdm")
    private String streetAddressCode;

    @JsonProperty("mlph")
    private String doorPlateNumber;

    @JsonProperty("xqmc")
    private String communityName;

    @JsonProperty("ldh")
    private String buildingNumber;

    @JsonProperty("dyh")
    private String unitNumber;

    @JsonProperty("lch")
    private String floorNumber;

    @JsonProperty("fjh")
    private String roomNumber;

    @JsonProperty("x")
    @NotNull(message = "经度不能为空")
    private BigDecimal longitude;

    @JsonProperty("y")
    @NotNull(message = "纬度不能为空")
    private BigDecimal latitude;

    @JsonProperty("geometry")
    private String geometry;

    @JsonProperty("pilot_area_1")
    private Integer pilotArea1;

    @JsonProperty("pilot_area_2")
    private Integer pilotArea2;

    @JsonProperty("area")
    private BigDecimal area;

    @JsonProperty("cjsj")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonProperty("gxsj")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
