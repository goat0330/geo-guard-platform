/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 三方预警处置列表响应
 */
@Data
@ExcelIgnoreUnannotated
public class ThirdPartyWarningDisposalVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 三方预警ID（内部使用，前端可忽略）
     */
    @JsonIgnore
    private String warningId;

    /**
     * 监测点名称
     */
    @ExcelProperty(value = "监测点名称")
    private String monitorPointName;

    /**
     * 行政区划
     */
    @ExcelProperty(value = "行政区划")
    private String administrativeRegion;

    /**
     * 监测点纬度
     */
    @ExcelProperty(value = "监测点纬度")
    private Double latitude;

    /**
     * 监测点经度
     */
    @ExcelProperty(value = "监测点经度")
    private Double longitude;

    /**
     * 预警设备名称
     */
    @ExcelProperty(value = "预警设备名称")
    private String warningDeviceName;

    /**
     * 本地设备ID，供前端联动设备详情使用
     */
    private String deviceId;

    /**
     * 设备主键 ID
     */
    private String id;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备状态
     */
    private Long deviceStatus;

    /**
     * MQTT/平台客户端标识
     */
    private String clientId;

    /**
     * 设备序列号（SN）
     */
    private String deviceSerialNumber;

    /**
     * 设备登录用户名
     */
    private String deviceLoginUsername;

    /**
     * 全局唯一标识
     */
    private String globalUniqueId;

    /**
     * 设备参数（JSON 或配置串）
     */
    private String deviceParameters;

    /**
     * 设备业务编码
     */
    private String deviceBusinessCode;

    /**
     * 关联监测点 ID
     */
    private String monitoringPointId;

    /**
     * 接入协议
     */
    private String accessProtocol;

    /**
     * 通信方式
     */
    private String communicationMethod;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 设备型号
     */
    private String deviceModel;

    /**
     * 设备安装地址
     */
    private String deviceInstallAddress;

    /**
     * 物联网卡号
     */
    private String iotCardNumber;

    /**
     * 监测类型
     */
    private String monitoringType;

    /**
     * 是否启用（如：Y/N）
     */
    private String isEnabled;

    /**
     * 启用时间
     */
    private Date deviceEnableTime;

    /**
     * 最后在线时间
     */
    private Date deviceLastOnlineTime;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 更新时间
     */
    private Date updatedTime;

    /**
     * 设备类别
     */
    private Long deviceCategory;

    /**
     * 是否同步到大屏（0/1）
     */
    private Long isSyncToScreen;

    /**
     * 设备最新在线时间（运行态刷新）
     */
    private Date deviceLatestOnlineTime;

    /**
     * 批量同步设备编码
     */
    private String batchSyncDeviceCode;

    /**
     * 网关设备 SN
     */
    private String gatewayDeviceSn;

    /**
     * 灾害类型
     */
    private String disasterType;

    /**
     * 运维单位
     */
    private String operationMaintenanceUnit;

    /**
     * 关联斜坡单元 ID
     */
    private String slopeUnitId;

    /**
     * 斜坡单元名称
     */
    private String slopeUnitName;

    /**
     * 试点区域标识 1
     */
    private Integer pilotArea1;

    /**
     * 试点区域标识 2
     */
    private Integer pilotArea2;

    /**
     * 省
     */
    private String province;

    /**
     * 市
     */
    private String city;

    /**
     * 县（区）
     */
    private String county;

    /**
     * 街道（乡镇）
     */
    private String street;

    /**
     * 村（社区）
     */
    private String village;

    /**
     * 省级行政区划编码
     */
    private String provinceCode;

    /**
     * 市级行政区划编码
     */
    private String cityCode;

    /**
     * 县级行政区划编码
     */
    private String countyCode;

    /**
     * 街道级行政区划编码
     */
    private String streetCode;

    /**
     * 村级行政区划编码
     */
    private String villageCode;

    /**
     * 预警等级（内部编码统一为数值越大预警等级越高：1=蓝色 2=黄色 3=橙色 4=红色；导出时需按此编码解释）
     */
    @ExcelProperty(value = "预警等级: 1.蓝色,2.黄色,3.橙色,4.红色")
    private Integer warningLevel;

    /**
     * 预警发布时间
     */
    @ExcelProperty(value = "预警发布时间")
    private String warningPublishTime;

    /**
     * 处置类型
     */
    @ExcelProperty(value = "处置类型: 0.数据异常导致误报, 1.设备维护导致误报, 2.设备遭到破坏, 3.正常预警, 4.预警模型待优化")
    private String disposalType;

    /**
     * 处置人
     */
    @ExcelProperty(value = "处置人")
    private String disposalPerson;

    /**
     * 预警处置时间
     */
    @ExcelProperty(value = "预警处置时间")
    private String warningDisposalTime;

    /**
     * 是否有效预警: 1-有效, 0-无效
     */
    @ExcelProperty(value = "是否有效预警")
    private Integer validWarning;

    /**
     * 最近一条预警时间（yjsj）
     */
    private String warningTime;

    /**
     * 从在线状态名称推断的在线状态，0-离线，1-在线
     */
    private Integer onlineStatus;
}
