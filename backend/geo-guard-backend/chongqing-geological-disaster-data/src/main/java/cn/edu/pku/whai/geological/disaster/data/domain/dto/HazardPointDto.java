/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.dto;

import cn.edu.pku.whai.geological.disaster.data.domain.po.HazardPoint;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


@Data
@AutoMapper(target = HazardPoint.class, reverseConvertGenerate = false)
public class HazardPointDto implements Serializable {

    @JsonProperty("id")
    private String id;

    @JsonProperty("yhdmc") // 对应 JSON 中的 yhdmc
    private String name;

    @JsonProperty("yhdlx") // 对应 JSON 中的 yhdlx
    private String typeCode;

    @JsonProperty("wgbh")
    private String gridCode;

    @JsonProperty("zhdwybh")
    private String uniqueDisasterId;

    @JsonProperty("sheng")
    private String province;

    @JsonProperty("shi")
    private String city;

    @JsonProperty("xian")
    private String county;

    @JsonProperty("xiang")
    private String street;

    @JsonProperty("cun")
    private String village;

    @JsonProperty("wgz")
    private String gridGroup;

    @JsonProperty("xzb")
    private Double xCoordinate;

    @JsonProperty("yzb")
    private Double yCoordinate;

    @JsonProperty("jd")
    private Double longitude;

    @JsonProperty("wd")
    private Double latitude;

    @JsonProperty("wkt")
    private String wkt;

    @JsonProperty("chang")
    private Double lengthM;

    @JsonProperty("kuan")
    private Double widthM;

    @JsonProperty("gao")
    private Double heightM;

    @JsonProperty("mj")
    private Double areaSqm;

    @JsonProperty("tj")
    private Double volumeCbm;

    @JsonProperty("gmdj")
    private String scaleGrade;

    @JsonProperty("glcj")
    private String managementLevel;

    @JsonProperty("wxrk")
    private Integer threatenedPopulation;

    @JsonProperty("wxcc")
    private Integer threatenedPropertyValue;

    @JsonProperty("xqdj")
    private String riskGrade;

    @JsonProperty("cjzhsj")
    private String disasterHistoryTime;

    @JsonProperty("dzhjtj")
    private String geologicalEnvironment;

    @JsonProperty("bxtz")
    private String deformationFeatures;

    @JsonProperty("wdxfx")
    private String stabilityAnalysis;

    @JsonProperty("wdxzt")
    private String stabilityStatus;

    @JsonProperty("wdxqs")
    private String stabilityTrend;

    @JsonProperty("yfys")
    private String triggerFactors;

    @JsonProperty("qzwh")
    private String potentialHazards;

    @JsonProperty("lzztyc")
    private String preDisasterPrediction;

    @JsonProperty("jcff")
    private String monitoringMethod;

    @JsonProperty("jcrwgryid")
    private String monitoringPersonId;

    @JsonProperty("tbrq")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date reportDate;

    @JsonProperty("yhdhissn")
    private String historySn;

    @JsonProperty("flag")
    private Integer dataFlag;

    @JsonProperty("sfhx")
    private Integer isCancelled;

    @JsonProperty("sqlb")
    private String operationType;

    @JsonProperty("shzt")
    private String reviewStatus;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdTime;

    @JsonProperty("dcsj")
    private Integer hasSurveyData;
}
