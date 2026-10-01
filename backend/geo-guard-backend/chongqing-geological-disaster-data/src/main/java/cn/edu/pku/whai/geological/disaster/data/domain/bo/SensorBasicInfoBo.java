/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SensorBasicInfo;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 传感器基础信息业务对象 v_sensor_basic_info
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SensorBasicInfo.class, reverseConvertGenerate = false)
public class SensorBasicInfoBo extends BaseEntity {

    /**
     * 传感器唯一主键ID（业务唯一标识）
     */

    private String id;

    /**
     * 传感器编号（如"CGQ420305001"）
     */

    private String sensorCode;

    /**
     * 关联设备ID（关联JC_BA10_SB表的ID字段）
     */

    private String deviceId;

    /**
     * 关联监测点ID（关联JC_BA10_JCD表的ID字段）
     */

    private String monitoringPointId;

    /**
     * 客户端ID（平台接入标识）
     */

    private String clientId;

    /**
     * 监测方法编码（01=自动监测、02=人工监测等）
     */

    private String monitoringMethodCode;

    /**
     * 监测类型编码（01=倾角、02=位移、03=雨量等）
     */

    private String monitoringTypeCode;

    /**
     * 传感器名称（如"黄石老火车站滑坡倾角传感器"）
     */

    private String sensorName;

    /**
     * 传感器参数值（存储核心配置参数）
     */

    private String sensorParameterValue;

    /**
     * 分辨率（传感器测量精度，如0.01°）
     */

    private String resolution;

    /**
     * 灵敏度（传感器感应灵敏度）
     */

    private String sensitivity;

    /**
     * 设备参数（冗余字段，与SB表SBCS语义一致）
     */

    private String equipmentParameters;

    /**
     * 入库日期（传感器录入系统时间）
     */

    private Date storageDate;

    /**
     * 通信方式（4G/5G/有线/LoRa等）
     */

    private String communicationMethod;

    /**
     * 监测系统图标（前端展示图标路径/编码）
     */

    private String monitoringSystemIcon;

    /**
     * 计量单位（如"°（度）、mm（毫米）"）
     */

    private String measurementUnit;

    /**
     * 安装纬度（高精度，保留8位小数）
     */

    private BigDecimal installLatitude;

    /**
     * 安装经度（高精度，保留8位小数）
     */

    private BigDecimal installLongitude;

    /**
     * 安装海拔（单位：米）
     */

    private String installAltitude;

    /**
     * 传感器型号（出厂型号，如XJ-QL-01）
     */

    private String sensorModel;

    /**
     * 安装单位（负责安装的施工/运维单位）
     */

    private String installationUnit;

    /**
     * 安装时间（含时分秒的时间戳）
     */

    private Date installationTime;

    /**
     * 数据精度（如"0.01"，测量数据精度）
     */

    private String dataPrecision;

    /**
     * 采集频率（如"5s"，数据采集间隔）
     */

    private String collectionFrequency;

    /**
     * 上传频率（如"10s"，数据上传间隔）
     */

    private String uploadFrequency;

    /**
     * 上报频率（设备级上报间隔）
     */

    private String deviceReportFrequency;

    /**
     * 报警频率（预警触发后的上报间隔）
     */

    private String alarmReportFrequency;

    /**
     * 安装位置（详细安装点位描述）
     */

    private String installationLocation;

    /**
     * 阈值（预警触发阈值）
     */

    private String thresholdValue;

    /**
     * 最小值（测量范围下限）
     */

    private String measurementMinValue;

    /**
     * 最大值（测量范围上限）
     */

    private String measurementMaxValue;

    /**
     * 监测型号（冗余字段，与CGQXH语义一致）
     */

    private String monitoringModel;

    /**
     * 最新采集时间（传感器最后一次采集数据时间）
     */

    private Date latestCollectionTime;

    /**
     * 创建人（原字段拼写错误，修正为标准命名）
     */

    private String createdBy;

    /**
     * 创建时间（原字段拼写错误，修正为标准命名）
     */

    private Date createdTime;

    /**
     * 更新人（最后修改人ID/姓名）
     */

    private String updatedBy;

    /**
     * 更新时间（最后修改时间）
     */

    private Date updatedTime;

    /**
     * 是否打开（1=是、0=否）
     */

    private String isEnabled;

    /**
     * 开始值（测量起始值）
     */

    private BigDecimal measurementStartValue;

    /**
     * 结束值（测量终止值）
     */

    private BigDecimal measurementEndValue;

    /**
     * 是否同步到大屏（1=是、0=否）
     */

    private Long isSyncToScreen;

    /**
     * 批量同步传感器编号（批量操作时的传感器标识）
     */

    private String batchSyncSensorCode;


}
