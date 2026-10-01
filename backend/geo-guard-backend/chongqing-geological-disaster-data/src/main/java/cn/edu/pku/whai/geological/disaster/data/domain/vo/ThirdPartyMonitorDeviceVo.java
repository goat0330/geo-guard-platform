/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 三方监测设备分页记录
 */
@Data
public class ThirdPartyMonitorDeviceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备主键
     */
    private String id;

    /**
     * 设备客户端ID
     */
    private String clientId;

    /**
     * 创建设备时间
     */
    private String createTime;

    /**
     * 地理位置
     */
    private String locationDesc;

    /**
     * 设备型号
     */
    private String deviceModel;

    /**
     * 经度
     */
    private Double lon;

    /**
     * 设备厂商
     */
    private String manufacturer;

    /**
     * 设备厂商ID
     */
    private String manufacturerId;

    /**
     * 上次在线时间
     */
    private String lastOnlineTime;

    /**
     * 是否启用
     */
    private String enabledFlag;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 设备启用时间
     */
    private String deviceEnableTime;

    /**
     * SN号
     */
    private String sn;

    /**
     * 设备状态名称
     */
    private String deviceStatusName;

    /**
     * 地址
     */
    private String address;

    /**
     * 纬度
     */
    private Double lat;

    /**
     * 设备编号
     */
    private String deviceCode;

    /**
     * 监测点ID
     */
    private String monitorPointId;

    /**
     * 边坡厅设备编号
     */
    private String platformDeviceCode;

    /**
     * 通讯方式
     */
    private String communicationType;

    /**
     * 监测点名称
     */
    private String monitorPointName;

    /**
     * 是否同步到大屏
     */
    private Integer syncToBigScreen;

    /**
     * 行号
     */
    private Integer rowId;

    /**
     * 设备key
     */
    private String deviceKey;

    /**
     * 监测类型
     */
    private String monitorType;

    /**
     * 设备种类
     */
    private Integer deviceCategory;

    /**
     * 接入协议
     */
    private String accessProtocol;

    /**
     * 设备状态
     */
    private Integer deviceStatus;

    /**
     * 在线状态时间
     */
    private String onlineStatusTime;
}
