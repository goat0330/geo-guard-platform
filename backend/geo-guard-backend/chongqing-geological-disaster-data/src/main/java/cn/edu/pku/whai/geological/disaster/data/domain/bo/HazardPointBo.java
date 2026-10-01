/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;


import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.data.domain.po.HazardPoint;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 隐患点基本情况业务对象 data_hazard_point
 *
 * @author kongweiguang
 * @date 2026-01-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = HazardPoint.class, reverseConvertGenerate = false)
public class HazardPointBo extends BaseEntity {


    @Serial
    private static final long serialVersionUID = 1L;


    /**
     * 主键
     */
    @NotBlank(message = "主键不能为空", groups = {EditGroup.class})
    private String id;


    /**
     * 隐患点名称
     */
    private String name;

    /**
     * 隐患点类型
     */
    private String typeCode;

    /**
     * 网格编号（与网格管理表grid_code命名统一）
     */
    private String gridCode;

    /**
     * 灾害点唯一编号
     */
    private String uniqueDisasterId;

    /**
     * 省
     */
    private String province;

    /**
     * 市
     */
    private String city;

    /**
     * 县
     */
    private String county;

    /**
     * 乡/街道
     */
    private String street;

    /**
     * 村
     */
    private String village;

    /**
     * 网格组
     */
    private String gridGroup;

    /**
     * X坐标
     */
    private BigDecimal xCoordinate;

    /**
     * Y坐标
     */
    private BigDecimal yCoordinate;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 空间范围WKT
     */
    private String wkt;

    /**
     * 长
     */
    private BigDecimal lengthM;

    /**
     * 宽
     */
    private BigDecimal widthM;

    /**
     * 高
     */
    private BigDecimal heightM;

    /**
     * 面积
     */
    private BigDecimal areaSqm;

    /**
     * 体积
     */
    private BigDecimal volumeCbm;

    /**
     * 规模等级
     */
    private String scaleGrade;

    /**
     * 管理层级
     */
    private String managementLevel;

    /**
     * 威胁人口
     */
    private Integer threatenedPopulation;

    /**
     * 威胁财产
     */
    private Integer threatenedPropertyValue;

    /**
     * 险情等级
     */
    private String riskGrade;

    /**
     * 曾经发生灾害时间
     */
    private String disasterHistoryTime;

    /**
     * 地质环境条件
     */
    private String geologicalEnvironment;

    /**
     * 变形特征及活动历史
     */
    private String deformationFeatures;

    /**
     * 稳定性分析
     */
    private String stabilityAnalysis;

    /**
     * 稳定性状态
     */
    private String stabilityStatus;

    /**
     * 稳定性趋势
     */
    private String stabilityTrend;

    /**
     * 引发因数
     */
    private String triggerFactors;

    /**
     * 潜在危害
     */
    private String potentialHazards;

    /**
     * 临灾状态预测
     */
    private String preDisasterPrediction;

    /**
     * 监测方法
     */
    private String monitoringMethod;

    /**
     * 监测人网格人员关联ID
     */
    private String monitoringPersonId;

    /**
     * 填表日期
     */
    private Date reportDate;

    /**
     * 隐患点历史标识
     */
    private String historySn;

    /**
     * 数据标识
     */
    private Integer dataFlag;

    /**
     * 是否核销（0=未核销，1=已核销）
     */
    private Integer isCancelled;

    /**
     * 申请类别
     */
    private String operationType;

    /**
     * 审核状态
     */
    private String reviewStatus;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 是否有调查数据（0=无，1=有）
     */
    private Integer hasSurveyData;

    /**
     * 隐患点分类
     */
    private String category;

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
}
