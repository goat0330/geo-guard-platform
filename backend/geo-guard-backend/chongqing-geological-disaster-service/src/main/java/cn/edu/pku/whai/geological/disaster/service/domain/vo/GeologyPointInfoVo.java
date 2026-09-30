/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.EngineeringGeology;
import cn.edu.pku.whai.geological.disaster.data.domain.po.HydroGeology;
import cn.edu.pku.whai.geological.disaster.data.domain.po.Stratum;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataGeologicalFaultVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataGeologicalFoldVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.GeologyAdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HazardPointVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.LandPlanningVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.PointHouseInfoVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RoadStatisticVo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 一标三十点位地质信息聚合视图
 *
 * @author zhuzc
 * @date 2026-06-04
 */
@Data
public class GeologyPointInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 斜坡单元ID
     */
    private String slopeUnitId;

    /**
     * 斜坡单元名称
     */
    private String slopeUnitName;

    /**
     * 村地址
     */
    private String villageAddress;

    /**
     * 中心点坐标
     */
    private String center;

    /**
     * 斜坡类型
     */
    private String slopeType;

    /**
     * 涉水河流、沟谷
     */
    private String waterRiverValley;

    /**
     * 斜坡地形地貌、河流冲沟切割及斜坡形体特征
     */
    private String landformFeature;

    /**
     * 斜坡地层岩性
     */
    private String stratumLithology;

    /**
     * 斜坡组合结构类型及特征
     */
    private String structureFeature;

    /**
     * 斜坡水文地质条件
     */
    private String slopeHydrogeology;

    /**
     * 斜坡土地及植被
     */
    private String landVegetation;

    /**
     * 控制崩滑结构面
     */
    private String controlStructuralPlane;

    /**
     * 斜坡稳定性影响因素
     */
    private String stabilityFactors;

    /**
     * 斜坡稳定性分析现状
     */
    private String stabilityCurrent;

    /**
     * 斜坡稳定性分析趋势
     */
    private String stabilityTrend;

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
    private String seismicIntensityZoning;

    /**
     * 地震动峰值加速度分区
     */
    private String seismicPeakGroundAccelerationZoning;

    /**
     * 建筑数量（栋）——③+ 斜坡单元ID 关联
     */
    private Long houseCount;

    /**
     * 人口底数（人）——③+ 斜坡单元ID 关联
     */
    private Long personCount;

    /**
     * 建筑数量（栋）——⑧ 坐标点 10m 范围（ST_DWithin），房屋数=户数
     */
    private Long houseCountByPoint;

    /**
     * 人口底数（人）——⑧ 坐标点 10m 范围（ST_DWithin）
     */
    private Long personCountByPoint;

    /**
     * 点命中的房屋信息
     */
    private PointHouseInfoVo pointHouseInfo;

    /**
     * 最近水系名称
     */
    private String waterSystemName;

    /**
     * 最近道路名称
     */
    private String roadName;

    /**
     * 点命中的灾害点
     */
    private HazardPointVo hazardPoint;

    /**
     * 点最近的地质褶皱（data_geological_fold）
     */
    private DataGeologicalFoldVo fold;

    /**
     * 点最近的地质断层（data_geological_fault）
     */
    private DataGeologicalFaultVo fault;

    /**
     * 斜坡单元内部道路统计
     */
    private List<RoadStatisticVo> roadStatistics = new ArrayList<>();

    /**
     * 斜坡单元内部道路总长度（米）
     */
    private BigDecimal roadTotalLengthMeters;

    /**
     * 地貌类型（data_geomorphological.type）
     */
    private String geomorphologyType;

    /**
     * 地貌特征（data_geomorphological.feature）
     */
    private String geomorphologyFeature;

    /**
     * 点命中的国土空间规划用地类型
     */
    private String landPlanningType;

    /**
     * 斜坡单元涉及的用地类型及覆盖面积统计
     */
    private List<LandPlanningVo> slopeUnitLandPlanningStatistics = new ArrayList<>();
}
