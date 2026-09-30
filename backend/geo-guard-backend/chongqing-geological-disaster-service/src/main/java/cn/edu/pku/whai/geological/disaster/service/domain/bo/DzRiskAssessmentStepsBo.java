/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessmentSteps;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 地质灾害预测与易发性评价数据业务对象 dz_risk_assessment_steps
 *
 * @author kongweiguang
 * @date 2026-01-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzRiskAssessmentSteps.class, reverseConvertGenerate = false)
public class DzRiskAssessmentStepsBo extends BaseEntity {

    /**
     * 自增主键
     */
    @NotNull(message = "自增主键不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 原始对象ID (OBJECTID)
     */
    private String originalObjectId;

    /**
     * 栅格代码 (gridcode)
     */
    private Integer gridCode;

    /**
     * 区域面积 (area)
     */
    private Double areaSize;

    /**
     * 形状长度 (Shape_Leng)
     */
    private Double shapeLength;

    /**
     * 描述信息 (描述)
     */
    private String description;

    /**
     * MP面积 (mpArea)
     */
    private Double mpArea;

    /**
     * MP周长 (mpPerimete)
     */
    private Double mpPerimeter;

    /**
     * 点号 (点号)
     */
    private Integer pointNo;

    /**
     * 稳定性评价 (稳定性)
     */
    private String stability;

    /**
     * 结构子类 (结构2)
     */
    private String structureTypeSub;

    /**
     * 斜坡名称 (斜坡名)
     */
    private String slopeName;

    /**
     * 辅助长度1 (Shape_Le_1)
     */
    private Double shapeLength1;

    /**
     * 结构值/拼音 (jiegou)
     */
    private Integer structureVal;

    /**
     * 滑坡易发性标识 (滑坡易)
     */
    private Integer landslideProne;

    /**
     * 易发性拼音字段 (huapoyifa)
     */
    private Integer landslidePronePy;

    /**
     * 辅助长度2 (Shape_Le_2)
     */
    private Double shapeLength2;

    /**
     * 形状面积 (Shape_Area)
     */
    private Double shapeArea;

    /**
     * 斜坡结构描述 (斜坡结构)
     */
    private String slopeStructure;

    /**
     * 平均高程 (elevation_mean)
     */
    private Double elevationMean;

    /**
     * 最大高程 (elevation_max)
     */
    private Integer elevationMax;

    /**
     * 最小高程 (elevation_min)
     */
    private Integer elevationMin;

    /**
     * 相对高差 (elevation_diff)
     */
    private Integer elevationDiff;

    /**
     * 平均坡度 (slope_mean)
     */
    private String slopeMean;

    /**
     * 平均坡向 (aspect_mean)
     */
    private Double aspectMean;

    /**
     * 平面曲率 (plan_curvature)
     */
    private Double planCurvature;

    /**
     * 剖面曲率 (profile_curvature)
     */
    private Double profileCurvature;

    /**
     * 岩性组说明 (SHUOMING)
     */
    private String lithologyDesc;

    /**
     * 易发性数值 (易发值)
     */
    private Integer susceptibilityVal;

    /**
     * 历史灾害点ID列表 (灾害点列表)
     */
    private List<Integer> disasterPoints;

    /**
     * 斜坡形态 (斜坡形态)
     */
    private String slopeMorphology;

    /**
     * 斜坡单元形态 (斜坡单元形态)
     */
    private String unitMorphology;

    /**
     * 植被覆盖度文本 (植被覆盖度)
     */
    private String vegetationCover;

    /**
     * 是否邻水/距离范围 (是否邻水)
     */
    private String adjacentWater;

    /**
     * 构造距离范围 (构造)
     */
    private String tectonicDist;

    /**
     * 斜坡结构编码
     */
    private Integer slopeStructureCode;

    /**
     * 说明/岩性编码
     */
    private Integer shuomingCode;

    /**
     * 易发性编码
     */
    private Integer susceptibilityCode;

    /**
     * 斜坡形态编码
     */
    private Integer slopeMorphCode;

    /**
     * 单元形态编码
     */
    private Integer unitMorphCode;

    /**
     * 植被覆盖编码
     */
    private Integer vegetationCoverCode;

    /**
     * 邻水编码
     */
    private Integer adjacentToWaterCode;

    /**
     * 结构编码
     */
    private Integer structureCode;

    /**
     * dz_risk_assessment表id
     */
    private Long assessmentId;

    /**
     * 易损性:人口数量
     */
    private Double popCount;

    /**
     * 易损性:经济总值
     */
    private Double econSum;

    /**
     * 危险性:道路密度
     */
    private Double roadDensity;

    /**
     * 危险性:建筑密度
     */
    private Double buildingDensity;

    /**
     * 易损性:人口密度
     */
    private Double popDensity;

    /**
     * 危险性:降雨概率
     */
    private Double rainfallProb;

    /**
     * 过去7天降雨量总和
     */
    private Double rainfallPast7;

    /**
     * 过去7天逐日降雨量（JSON数组）
     */
    private List<Object> rainfallPast7Daily;

    /**
     * 未来一天预报降雨量
     */
    private Double rainfallForecast;

    /**
     * 危险性:时间概率合成值 (combined_time_prob)
     */
    private Double combinedTimeProb;

    /**
     * 易损性展示JSON (vulnerability_display_json)
     */
    private Object vulnerabilityDisplayJson;
}
