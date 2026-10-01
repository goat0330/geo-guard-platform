/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.dto;

import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorPoint;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

/**
 * @author lizheng
 * @date 2026-01-06
 */
@Data
@AutoMapper(target = MonitorPoint.class, reverseConvertGenerate = false)
public class MonitorPointDto {
    private static final long serialVersionUID = 1L;

    /**
     * 监测点 ID
     */
    @JsonProperty("jcdid")
    private String monitorPointId;

    /**
     * 监测点名称
     */
    @JsonProperty("jcdname")
    private String monitorName;

    /**
     * 隐患点/基本信息 ID
     */
    @JsonProperty("jbxxid")
    private String basicInfoId;

    /**
     * 地理位置描述
     */
    @JsonProperty("dlwz")
    private String locationDescription;

    /**
     * 灾害类型名称 (如：滑坡)
     */
    @JsonProperty("zhlxmc")
    private String disasterTypeName;

    /**
     * 灾害类型编码 (如：01)
     */
    @JsonProperty("zhlx")
    private String disasterTypeCode;

    /**
     * 监测点编号
     */
    @JsonProperty("jcdbh")
    private String monitorCode;

    /**
     * 经度
     */
    @JsonProperty("lon")
    private Double longitude;

    /**
     * 纬度
     */
    @JsonProperty("lat")
    private Double latitude;

    /**
     * 填报单位名称
     */
    @JsonProperty("tbdwmc")
    private String reportUnitName;

    /**
     * 运维/业务单位名称
     */
    @JsonProperty("ywdw")
    private String serviceUnitName;

    /**
     * 专业监测预警员姓名
     */
    @JsonProperty("zyjcyjy")
    private String professionalMonitorName;

    /**
     * 专业监测预警员电话 (加密)
     */
    @JsonProperty("zyjcyjydh")
    private String professionalMonitorPhone;

    /**
     * 群测群防员姓名
     */
    @JsonProperty("qcqfyxm")
    private String massPreventionName;

    /**
     * 群测群防员电话 (加密)
     */
    @JsonProperty("qcqfydh")
    private String massPreventionPhone;

    /**
     * 防治负责人姓名
     */
    @JsonProperty("fzzrr")
    private String responsiblePersonName;

    /**
     * 防治负责人电话 (加密)
     */
    @JsonProperty("fzzrrdh")
    private String responsiblePersonPhone;

    /**
     * 距离 (通常接口返回为 null 或数值)
     */
    @JsonProperty("distance")
    private Double distance;
}
