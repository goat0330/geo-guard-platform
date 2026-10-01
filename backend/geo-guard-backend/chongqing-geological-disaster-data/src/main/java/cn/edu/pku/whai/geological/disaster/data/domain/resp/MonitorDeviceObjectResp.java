/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.resp;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 监测设备对象响应类
 *
 * @author lizheng
 * @date 2026-01-21
 */
@Data
public class MonitorDeviceObjectResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
    private String id;

    /**
     * 设备名称
     */
    @JsonAlias("sbname")
    private String deviceName;

    /**
     * 设备状态
     */
    @JsonAlias("sbzt")
    private Integer deviceStatus;

    /**
     * 客户端ID
     */
    @JsonAlias("clientid")
    private String clientId;

    /**
     * 设备密钥
     */
    @JsonAlias("sbkey")
    private String deviceSecret;

    /**
     * 序列号
     */
    @JsonAlias("sn")
    private String serialNumber;

    /**
     * 密码
     */
    @JsonAlias("pwd")
    private String password;

    /**
     * 加密密码
     */
    @JsonAlias("jmhpwd")
    private String encryptedPassword;

    /**
     * 用户名
     */
    @JsonAlias("yhm")
    private String username;

    /**
     * 全局唯一标识
     */
    @JsonAlias("zguid")
    private String globalUniqueId;

    /**
     * 设备参数
     */
    @JsonAlias("sbcs")
    private String deviceParameters;

    /**
     * 设备编号
     */
    @JsonAlias("sbbh")
    private String deviceBusinessCode;

    /**
     * 监测点ID
     */
    @JsonAlias("jcdid")
    private String monitoringPointId;

    /**
     * 接入协议
     */
    @JsonAlias("jrxy")
    private String accessProtocol;

    /**
     * 通信方式
     */
    @JsonAlias("txfs")
    private String communicationMethod;

    /**
     * 设备类型
     */
    @JsonAlias("sbtype")
    private String deviceType;

    /**
     * 设备型号
     */
    @JsonAlias("sbxh")
    private String deviceModel;

    /**
     * 经度
     */
    @JsonAlias("lon")
    private Double longitude;

    /**
     * 纬度
     */
    @JsonAlias("lat")
    private Double latitude;

    /**
     * 地址
     */
    @JsonAlias("addr")
    private String address;

    /**
     * 卡号
     */
    @JsonAlias("cardnum")
    private String cardNumber;

    /**
     * 监测类型
     */
    @JsonAlias("jclx")
    private String monitorType;

    /**
     * 是否启用
     */
    @JsonAlias("sfqy")
    private String isEnabled;

    /**
     * 设备启用时间
     */
    @JsonAlias("sbqysj")
    private String deviceEnableTime;

    /**
     * 上次在线时间
     */
    @JsonAlias("sczxsj")
    private String lastOnlineTime;

    /**
     * 设备质量
     */
    @JsonAlias("sbzl")
    private Integer deviceQuality;

    /**
     * 创建人
     */
    @JsonAlias("crearteBy")
    private String createBy;

    /**
     * 创建时间
     */
    @JsonAlias("crearteTime")
    private String createTime;

    /**
     * 更新人
     */
    @JsonAlias("updateBy")
    private String updateBy;

    /**
     * 更新时间
     */
    @JsonAlias("updateTime")
    private String updateTime;

    /**
     * 是否打开
     */
    @JsonAlias("sfdk")
    private String isOpen;

    /**
     * 系数
     */
    @JsonAlias("ks")
    private Double coefficient;

    /**
     * 厂商名称
     */
    @JsonAlias("csmc")
    private String manufacturerName;
}
