/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessmentSteps;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;


/**
 * 地质灾害预测与易发性评价数据视图对象 dz_risk_assessment_steps
 *
 * @author kongweiguang
 * @date 2026-01-19
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DzRiskAssessmentSteps.class)
public class DzRiskAssessmentStepsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 自增主键
     */
    @ExcelProperty(value = "自增主键")
    private Long id;

    /**
     * 原始对象ID (OBJECTID)
     */
    @ExcelProperty(value = "原始对象ID (OBJECTID)")
    private String originalObjectId;

    /**
     * 栅格代码 (gridcode)
     */
    @ExcelProperty(value = "栅格代码 (gridcode)")
    private Integer gridCode;

    /**
     * 区域面积 (area)
     */
    @ExcelProperty(value = "区域面积 (area)")
    private Double areaSize;

    /**
     * 形状长度 (Shape_Leng)
     */
    @ExcelProperty(value = "形状长度 (Shape_Leng)")
    private Double shapeLength;

    /**
     * 描述信息 (描述)
     */
    @ExcelProperty(value = "描述信息 (描述)")
    private String description;

    /**
     * MP面积 (mpArea)
     */
    @ExcelProperty(value = "MP面积 (mpArea)")
    private Double mpArea;

    /**
     * MP周长 (mpPerimete)
     */
    @ExcelProperty(value = "MP周长 (mpPerimete)")
    private Double mpPerimeter;

    /**
     * 点号 (点号)
     */
    @ExcelProperty(value = "点号 (点号)")
    private Integer pointNo;

    /**
     * 稳定性评价 (稳定性)
     */
    @ExcelProperty(value = "稳定性评价 (稳定性)")
    private String stability;

    /**
     * 结构子类 (结构2)
     */
    @ExcelProperty(value = "结构子类 (结构2)")
    private String structureTypeSub;

    /**
     * 斜坡名称 (斜坡名)
     */
    @ExcelProperty(value = "斜坡名称 (斜坡名)")
    private String slopeName;

    /**
     * 辅助长度1 (Shape_Le_1)
     */
    @ExcelProperty(value = "辅助长度1 (Shape_Le_1)")
    private Double shapeLength1;

    /**
     * 结构值/拼音 (jiegou)
     */
    @ExcelProperty(value = "结构值/拼音 (jiegou)")
    private Integer structureVal;

    /**
     * 滑坡易发性标识 (滑坡易)
     */
    @ExcelProperty(value = "滑坡易发性标识 (滑坡易)")
    private Integer landslideProne;

    /**
     * 易发性拼音字段 (huapoyifa)
     */
    @ExcelProperty(value = "易发性拼音字段 (huapoyifa)")
    private Integer landslidePronePy;

    /**
     * 辅助长度2 (Shape_Le_2)
     */
    @ExcelProperty(value = "辅助长度2 (Shape_Le_2)")
    private Double shapeLength2;

    /**
     * 形状面积 (Shape_Area)
     */
    @ExcelProperty(value = "形状面积 (Shape_Area)")
    private Double shapeArea;

    /**
     * 斜坡结构描述 (斜坡结构)
     */
    @ExcelProperty(value = "斜坡结构描述 (斜坡结构)")
    private String slopeStructure;

    /**
     * 平均高程 (elevation_mean)
     */
    @ExcelProperty(value = "平均高程 (elevation_mean)")
    private Double elevationMean;

    /**
     * 最大高程 (elevation_max)
     */
    @ExcelProperty(value = "最大高程 (elevation_max)")
    private Integer elevationMax;

    /**
     * 最小高程 (elevation_min)
     */
    @ExcelProperty(value = "最小高程 (elevation_min)")
    private Integer elevationMin;

    /**
     * 相对高差 (elevation_diff)
     */
    @ExcelProperty(value = "相对高差 (elevation_diff)")
    private Integer elevationDiff;

    /**
     * 平均坡度 (slope_mean)
     */
    @ExcelProperty(value = "平均坡度 (slope_mean)")
    private String slopeMean;

    /**
     * 平均坡向 (aspect_mean)
     */
    @ExcelProperty(value = "平均坡向 (aspect_mean)")
    private Double aspectMean;

    /**
     * 平面曲率 (plan_curvature)
     */
    @ExcelProperty(value = "平面曲率 (plan_curvature)")
    private Double planCurvature;

    /**
     * 剖面曲率 (profile_curvature)
     */
    @ExcelProperty(value = "剖面曲率 (profile_curvature)")
    private Double profileCurvature;

    /**
     * 岩性组说明 (SHUOMING)
     */
    @ExcelProperty(value = "岩性组说明 (SHUOMING)")
    private String lithologyDesc;

    /**
     * 易发性数值 (易发值)
     */
    @ExcelProperty(value = "易发性数值 (易发值)")
    private Integer susceptibilityVal;

    /**
     * 历史灾害点ID列表 (灾害点列表)
     */
    @ExcelProperty(value = "历史灾害点ID列表 (灾害点列表)")
    private List<Integer> disasterPoints;

    /**
     * 斜坡形态 (斜坡形态)
     */
    @ExcelProperty(value = "斜坡形态 (斜坡形态)")
    private String slopeMorphology;

    /**
     * 斜坡单元形态 (斜坡单元形态)
     */
    @ExcelProperty(value = "斜坡单元形态 (斜坡单元形态)")
    private String unitMorphology;

    /**
     * 植被覆盖度文本 (植被覆盖度)
     */
    @ExcelProperty(value = "植被覆盖度文本 (植被覆盖度)")
    private String vegetationCover;

    /**
     * 是否邻水/距离范围 (是否邻水)
     */
    @ExcelProperty(value = "是否邻水/距离范围 (是否邻水)")
    private String adjacentWater;

    /**
     * 构造距离范围 (构造)
     */
    @ExcelProperty(value = "构造距离范围 (构造)")
    private String tectonicDist;

    /**
     * 斜坡结构编码
     */
    @ExcelProperty(value = "斜坡结构编码")
    private Integer slopeStructureCode;

    /**
     * 说明/岩性编码
     */
    @ExcelProperty(value = "说明/岩性编码")
    private Integer shuomingCode;

    /**
     * 易发性编码
     */
    @ExcelProperty(value = "易发性编码")
    private Integer susceptibilityCode;

    /**
     * 斜坡形态编码
     */
    @ExcelProperty(value = "斜坡形态编码")
    private Integer slopeMorphCode;

    /**
     * 单元形态编码
     */
    @ExcelProperty(value = "单元形态编码")
    private Integer unitMorphCode;

    /**
     * 植被覆盖编码
     */
    @ExcelProperty(value = "植被覆盖编码")
    private Integer vegetationCoverCode;

    /**
     * 邻水编码
     */
    @ExcelProperty(value = "邻水编码")
    private Integer adjacentToWaterCode;

    /**
     * 结构编码
     */
    @ExcelProperty(value = "结构编码")
    private Integer structureCode;

    /**
     * dz_risk_assessment表id
     */
    @ExcelProperty(value = "dz_risk_assessment表id")
    private Long assessmentId;

    /**
     * 易损性:人口数量
     */
    @ExcelProperty(value = "易损性:人口数量")
    private Double popCount;

    /**
     * 易损性:经济总值
     */
    @ExcelProperty(value = "易损性:经济总值")
    private Double econSum;

    /**
     * 危险性:道路密度
     */
    @ExcelProperty(value = "危险性:道路密度")
    private Double roadDensity;

    /**
     * 危险性:建筑密度
     */
    @ExcelProperty(value = "危险性:建筑密度")
    private Double buildingDensity;

    /**
     * 易损性:人口密度
     */
    @ExcelProperty(value = "易损性:人口密度")
    private Double popDensity;

    /**
     * 危险性:降雨概率
     */
    @ExcelProperty(value = "危险性:降雨概率")
    private Double rainfallProb;

    /**
     * 过去7天降雨量总和
     */
    @ExcelProperty(value = "过去7天降雨量总和")
    private Double rainfallPast7;

    /**
     * 过去7天逐日降雨量（JSON数组）
     */
    @ExcelProperty(value = "过去7天逐日降雨量")
    private List<Object> rainfallPast7Daily;

    /**
     * 未来一天预报降雨量
     */
    @ExcelProperty(value = "未来一天预报降雨量")
    private Double rainfallForecast;

    /**
     * 危险性:时间概率合成值 (combined_time_prob)
     */
    @ExcelProperty(value = "危险性:时间概率合成值")
    private Double combinedTimeProb;

    /**
     * 易损性展示JSON (vulnerability_display_json)
     */
    @ExcelProperty(value = "易损性展示JSON")
    private Object vulnerabilityDisplayJson;
}
