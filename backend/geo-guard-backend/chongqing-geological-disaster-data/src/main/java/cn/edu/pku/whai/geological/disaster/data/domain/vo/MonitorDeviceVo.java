/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorDevice;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;


/**
 * 监测设备基础信息视图对象 v_monitor_device
 **/
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = MonitorDevice.class)
public class MonitorDeviceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备唯一主键ID
     */
    @ExcelProperty(value = "设备唯一主键ID")
    private String id;

    /**
     * 设备名称（如“黄石老火车站滑坡倾角传感器”）
     */
    @ExcelProperty(value = "设备名称", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "如=“黄石老火车站滑坡倾角传感器”")
    private String deviceName;

    /**
     * 设备状态（0=正常，1=故障，2=停用等）
     */
    @ExcelProperty(value = "设备状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0==正常，1=故障，2=停用等")
    private Long deviceStatus;

    /**
     * 客户端ID（设备接入平台标识）
     */
    @ExcelProperty(value = "客户端ID", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "设=备接入平台标识")
    private String clientId;

    /**
     * 设备密钥（接入鉴权用，敏感字段）
     */
    @ExcelProperty(value = "设备密钥", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "接=入鉴权用，敏感字段")
    private String deviceSecret;

    /**
     * 设备序列号（出厂编号，唯一标识硬件设备）
     */
    @ExcelProperty(value = "设备序列号", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "出=厂编号，唯一标识硬件设备")
    private String deviceSerialNumber;

    /**
     * 设备登录密码（敏感字段）
     */
    @ExcelProperty(value = "设备登录密码", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "敏=感字段")
    private String deviceLoginPassword;

    /**
     * 加密后密码（密码加密存储字段）
     */
    @ExcelProperty(value = "加密后密码", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "密=码加密存储字段")
    private String encryptedPassword;

    /**
     * 设备登录用户名
     */
    @ExcelProperty(value = "设备登录用户名")
    private String deviceLoginUsername;

    /**
     * 全局唯一标识（跨系统）
     */
    @ExcelProperty(value = "全局唯一标识", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "跨=系统")
    private String globalUniqueId;

    /**
     * 设备参数（JSON/拼接字符串，存储配置参数）
     */
    @ExcelProperty(value = "设备参数", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "J=SON/拼接字符串，存储配置参数")
    private String deviceParameters;

    /**
     * 设备编号（业务编码）
     */
    @ExcelProperty(value = "设备编号", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "业=务编码")
    private String deviceBusinessCode;

    /**
     * 关联监测点ID（关联JC_BA10_JCD表）
     */
    @ExcelProperty(value = "关联监测点ID", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "关=联JC_BA10_JCD表")
    private String monitoringPointId;

    /**
     * 接入协议（1=MQTT，2=HTTP，3=TCP等）
     */
    @ExcelProperty(value = "接入协议", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1==MQTT，2=HTTP，3=TCP等")
    private String accessProtocol;

    /**
     * 通信方式（1=4G，2=5G，3=有线等）
     */
    @ExcelProperty(value = "通信方式", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1==4G，2=5G，3=有线等")
    private String communicationMethod;

    /**
     * 设备类型（1=倾角，2=位移，3=雨量等）
     */
    @ExcelProperty(value = "设备类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1==倾角，2=位移，3=雨量等")
    private String deviceType;

    /**
     * 设备型号（出厂型号，如“XJ-QL-01”）
     */
    @ExcelProperty(value = "设备型号", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "出=厂型号，如“XJ-QL-01”")
    private String deviceModel;

    /**
     * 设备安装经度（高精度，保留10位小数）
     */
    @ExcelProperty(value = "设备安装经度", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "高=精度，保留10位小数")
    private BigDecimal longitude;

    /**
     * 设备安装纬度（高精度，保留10位小数）
     */
    @ExcelProperty(value = "设备安装纬度", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "高=精度，保留10位小数")
    private BigDecimal latitude;

    /**
     * 设备安装地址（详细描述）
     */
    @ExcelProperty(value = "设备安装地址", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "详=细描述")
    private String deviceInstallAddress;

    /**
     * 物联网卡号（4G/5G模块SIM卡号）
     */
    @ExcelProperty(value = "物联网卡号", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "4=G/5G模块SIM卡号")
    private String iotCardNumber;

    /**
     * 监测类型（如“滑坡监测、沉降监测”）
     */
    @ExcelProperty(value = "监测类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "如=“滑坡监测、沉降监测”")
    private String monitoringType;

    /**
     * 是否启用（1=是，0=否）
     */
    @ExcelProperty(value = "是否启用", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1==是，0=否")
    private String isEnabled;

    /**
     * 设备启用时间
     */
    @ExcelProperty(value = "设备启用时间")
    private Date deviceEnableTime;

    /**
     * 设备上次在线时间（判断设备是否离线）
     */
    @ExcelProperty(value = "设备上次在线时间", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "判=断设备是否离线")
    private Date deviceLastOnlineTime;

    /**
     * 创建人（原字段拼写错误，修正为标准命名）
     */
    @ExcelProperty(value = "创建人", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "原=字段拼写错误，修正为标准命名")
    private String createdBy;

    /**
     * 创建时间（原字段拼写错误，修正为标准命名）
     */
    @ExcelProperty(value = "创建时间", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "原=字段拼写错误，修正为标准命名")
    private Date createdTime;

    /**
     * 更新人
     */
    @ExcelProperty(value = "更新人")
    private String updatedBy;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updatedTime;

    /**
     * 设备种类（1=传感器，2=采集仪，3=网关等）
     */
    @ExcelProperty(value = "设备种类", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1==传感器，2=采集仪，3=网关等")
    private Long deviceCategory;

    /**
     * 是否同步到大屏（1=是，0=否）
     */
    @ExcelProperty(value = "是否同步到大屏", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1==是，0=否")
    private Long isSyncToScreen;

    /**
     * 最新在线时间（与SCZXSJ语义一致，冗余字段）
     */
    @ExcelProperty(value = "最新在线时间", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "与=SCZXSJ语义一致，冗余字段")
    private Date deviceLatestOnlineTime;

    /**
     * 批量同步设备编号
     */
    @ExcelProperty(value = "批量同步设备编号")
    private String batchSyncDeviceCode;

    /**
     * 网关设备SN号（关联网关设备时使用）
     */
    @ExcelProperty(value = "网关设备SN号", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "关=联网关设备时使用")
    private String gatewayDeviceSn;

    /**
     * 灾害类型（jcd.zhlx）
     */
    @ExcelProperty(value = "灾害类型")
    private String disasterType;

    /**
     * 运维单位（jcd.ywdw）
     */
    @ExcelProperty(value = "运维单位")
    private String operationMaintenanceUnit;

    /**
     * 斜坡单元ID（su.id AS slope_unit_id）
     */
    private String slopeUnitId;

    /**
     * 斜坡单元名称（su.name AS slope_unit_name）
     */
    private String slopeUnitName;

    /**
     * 试点区域1（su.pilot_area_1）
     */
    private Integer pilotArea1;

    /**
     * 试点区域2（su.pilot_area_2）
     */
    private Integer pilotArea2;

    /**
     * 省名称（su.province）
     */
    private String province;

    /**
     * 市名称（su.city）
     */
    private String city;

    /**
     * 县名称（su.county）
     */
    private String county;

    /**
     * 街道名称（su.street）
     */
    private String street;

    /**
     * 村名称（su.village）
     */
    private String village;

    /**
     * 省编码（su.province_code）
     */
    private String provinceCode;

    /**
     * 市编码（su.city_code）
     */
    private String cityCode;

    /**
     * 县编码（su.county_code）
     */
    private String countyCode;

    /**
     * 街道编码（su.street_code）
     */
    private String streetCode;

    /**
     * 村编码（su.village_code）
     */
    private String villageCode;

    /**
     * 最近一条报警等级，统一为内部编码：1=蓝色 2=黄色 3=橙色 4=红色
     */
    private Integer warningLevel;
}
