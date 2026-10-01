/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.EngineeringGeology;
import cn.edu.pku.whai.geological.disaster.data.domain.po.HydroGeology;
import cn.edu.pku.whai.geological.disaster.data.domain.po.Stratum;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 地质信息
 *
 * @author lizheng
 */
@Data
public class GeologyInfoVo {
    /**
     * 查询范围WKT
     */
    private String scopeWkt;

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

    private List<EngineeringGeology> engineeringGeologies = new ArrayList<>();
    private List<HydroGeology> hydroGeologies = new ArrayList<>();
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
     * 房屋数（栋）= 户数
     */
    private Long houseCount;

    /**
     * 人数
     */
    private Long personCount;

    /**
     * 道路统计
     */
    private List<RoadStatisticVo> roadStatistics = new ArrayList<>();

    /**
     * 道路总长度（米）
     */
    private BigDecimal roadTotalLengthMeters;

    /**
     * 灾害点列表
     */
    private List<HazardPointVo> hazardPoints = new ArrayList<>();

    /**
     * 斜坡单元涉及的用地类型及覆盖面积统计
     */
    private List<LandPlanningVo> slopeUnitLandPlanningStatistics = new ArrayList<>();
}
