/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.EngineeringGeology;
import cn.edu.pku.whai.geological.disaster.data.domain.po.HydroGeology;
import cn.edu.pku.whai.geological.disaster.data.domain.po.Stratum;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.GeologyAdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RoadStatisticVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGeologyVo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 事件应急调查报告聚合视图
 *
 * @author zhuzc
 * @date 2026-06-15
 */
@Data
public class EmergencyInvestigationReportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 处置任务现场记录完整数据
     */
    private DzTaskHandleSceneRecordVo sceneRecord;

    /**
     * 斜坡单元ID
     */
    private String slopeUnitId;

    /**
     * 斜坡单元名称
     */
    private String slopeUnitName;

    /**
     * 斜坡单元调查信息
     */
    private SlopeUnitGeologyVo slopeUnitGeology;

    /**
     * 工程地质列表
     */
    private List<EngineeringGeology> engineeringGeologies = new ArrayList<>();

    /**
     * 水文地质列表
     */
    private List<HydroGeology> hydroGeologies = new ArrayList<>();

    /**
     * 地层列表
     */
    private List<Stratum> stratums = new ArrayList<>();

    /**
     * 坐标点对应行政区划
     */
    private GeologyAdRegionVo adRegion;

    /**
     * 地震烈度分区
     */
    private String seismicIntensityZoning = "VI区";

    /**
     * 地震动峰值加速度分区
     */
    private String seismicPeakGroundAccelerationZoning = "0.05";

    /**
     * 发生前7天按 24 小时分段展示的历史降雨信息
     */
    private EmergencyHistoricalRainfallVo historicalRainfall;

    /**
     * 危险范围命中的道路统计信息
     */
    private List<RoadStatisticVo> hazardRoadStatistics = new ArrayList<>();

    /**
     * 风险范围命中的道路统计信息
     */
    private List<RoadStatisticVo> riskRoadStatistics = new ArrayList<>();
}
