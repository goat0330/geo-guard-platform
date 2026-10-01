/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 本地监测设备全量信息，附加三方「预警等级」「在线状态」；不包含密钥与密码等敏感字段。
 */
@Data
public class MonitorDeviceRuntimeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
     * 经度
     */
    private BigDecimal longitude;
    /**
     * 纬度
     */
    private BigDecimal latitude;
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
     * 最近一条预警的等级 1-4（已统一为内部编码：1=蓝色 2=黄色 3=橙色 4=红色；由三方 C1-C4 转换），无则 null
     */
    private Integer warningLevel;

    /**
     * 最近一条预警时间（yjsj）
     */
    private String warningTime;

    /**
     * 从 sbztname 解析的在线状态，0-离线，1-在线
     */
    private Integer onlineStatus;
}
