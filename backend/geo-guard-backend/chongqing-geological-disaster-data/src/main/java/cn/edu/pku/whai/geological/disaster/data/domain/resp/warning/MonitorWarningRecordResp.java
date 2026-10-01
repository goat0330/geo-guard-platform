/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.resp.warning;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 监测预警记录响应类
 */
@Data
public class MonitorWarningRecordResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 监测点ID
     */
    private String monitorPointId;

    /**
     * 监测点编号
     */
    private String monitorPointNum;

    /**
     * 监测点名称
     */
    private String monitorPointName;

    /**
     * 地址
     */
    private String address;

    /**
     * 经度
     */
    private Double lon;

    /**
     * 纬度
     */
    private Double lat;

    /**
     * 预警主键
     */
    private String warningPrimaryKey;

    /**
     * 预警等级（C1蓝色预警/C2黄色预警/C3橙色预警/C4红色预警）
     */
    private String warningLevel;

    /**
     * 预警时间
     */
    private String warningTime;

    /**
     * 预警处理状态
     */
    private String warningProcessStatus;

    /**
     * 预警处理结果
     */
    private String warningProcessResult;

    /**
     * 传感器ID
     */
    private String sensorId;

    /**
     * 预警公告路径
     */
    private String warningBullentinPath;
}
