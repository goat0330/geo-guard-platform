/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.resp;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 监测传感器对象响应类
 *
 * @author lizheng
 * @date 2026-01-21
 */
@Data
public class MonitorSensorObjectResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 传感器ID
     */
    private String id;

    /**
     * 传感器编号
     */
    @JsonAlias("cgqbh")
    private String sensorCode;

    /**
     * 设备ID
     */
    @JsonAlias("sbid")
    private String deviceId;

    /**
     * 监测点ID
     */
    @JsonAlias("jcdid")
    private String monitoringPointId;

    /**
     * 客户端ID
     */
    @JsonAlias("clientid")
    private String clientId;

    /**
     * 监测方法
     */
    @JsonAlias("jcff")
    private String monitorMethod;

    /**
     * 监测类型
     */
    @JsonAlias("jctype")
    private String monitorType;

    /**
     * 传感器名称
     */
    @JsonAlias("cgqname")
    private String sensorName;

    /**
     * 传感器初始值
     */
    @JsonAlias("cgqcsz")
    private String sensorInitialValue;

    /**
     * 分辨率
     */
    @JsonAlias("fbl")
    private String resolution;

    /**
     * 灵敏度
     */
    @JsonAlias("lmd")
    private String sensitivity;

    /**
     * 设备参数
     */
    @JsonAlias("sbcs")
    private String deviceParameters;

    /**
     * 入库日期
     */
    @JsonAlias("rkrq")
    private String entryDate;

    /**
     * 通信方式
     */
    @JsonAlias("txfs")
    private String communicationMethod;

    /**
     * 监测系统图标
     */
    @JsonAlias("jclxtb")
    private String monitorSystemIcon;

    /**
     * 单位
     */
    @JsonAlias("dw")
    private String unit;

    /**
     * 纬度
     */
    @JsonAlias("lat")
    private Double latitude;

    /**
     * 经度
     */
    @JsonAlias("lon")
    private Double longitude;

    /**
     * 海拔
     */
    @JsonAlias("alt")
    private Double altitude;

    /**
     * 传感器型号
     */
    @JsonAlias("cgqxh")
    private String sensorModel;

    /**
     * 安装单位
     */
    @JsonAlias("azdw")
    private String installUnit;

    /**
     * 安装时间
     */
    @JsonAlias("azsj")
    private String installTime;

    /**
     * 数据说明
     */
    @JsonAlias("sjsm")
    private String dataDescription;

    /**
     * 接收频率
     */
    @JsonAlias("jspl")
    private String receiveFrequency;

    /**
     * 采集频率
     */
    @JsonAlias("cjpl")
    private String collectFrequency;

    /**
     * 上报频率
     */
    @JsonAlias("sbpl")
    private String reportFrequency;

    /**
     * 基本频率
     */
    @JsonAlias("jbpl")
    private String baseFrequency;

    /**
     * 安装位置
     */
    @JsonAlias("azwz")
    private String installPosition;

    /**
     * 阈值
     */
    @JsonAlias("yz")
    private String threshold;

    /**
     * 最小值
     */
    @JsonAlias("min")
    private String minValue;

    /**
     * 最大值
     */
    @JsonAlias("max")
    private String maxValue;

    /**
     * 监测型号
     */
    @JsonAlias("jcxh")
    private String monitorModel;

    /**
     * 最新数据时间
     */
    @JsonAlias("zxsjsj")
    private String latestDataTime;

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
     * 秒数
     */
    @JsonAlias("ms")
    private String milliseconds;

    /**
     * 钻孔编号
     */
    @JsonAlias("zkbh")
    private String drillHoleNumber;

    /**
     * 字典名称
     */
    @JsonAlias("dicname")
    private String dictionaryName;

    /**
     * 状态
     */
    @JsonAlias("status")
    private String status;
}
