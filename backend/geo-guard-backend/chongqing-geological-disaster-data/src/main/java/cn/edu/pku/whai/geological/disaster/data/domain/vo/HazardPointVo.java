/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import cn.edu.pku.whai.geological.disaster.data.domain.po.HazardPoint;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;


/**
 * 隐患点、监测点基本情况视图对象 v_hazard_point
 **/
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = HazardPoint.class)
public class HazardPointVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private String id;

    /**
     * 隐患点名称
     */
    @ExcelProperty(value = "隐患点名称")
    private String name;

    /**
     * 隐患点类型
     */
    @ExcelProperty(value = "隐患点类型")
    private String typeCode;

    /**
     * 网格编号（与网格管理表grid_code命名统一）
     */
    @ExcelProperty(value = "网格编号", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "与=网格管理表grid_code命名统一")
    private String gridCode;

    /**
     * 灾害点唯一编号
     */
    @ExcelProperty(value = "灾害点唯一编号")
    private String uniqueDisasterId;

    /**
     * 省
     */
    @ExcelProperty(value = "省")
    private String province;

    /**
     * 市
     */
    @ExcelProperty(value = "市")
    private String city;

    /**
     * 县
     */
    @ExcelProperty(value = "县")
    private String county;

    /**
     * 乡/街道
     */
    @ExcelProperty(value = "乡/街道")
    private String street;

    /**
     * 村
     */
    @ExcelProperty(value = "村")
    private String village;

    /**
     * 网格组
     */
    @ExcelProperty(value = "网格组")
    private String gridGroup;

    /**
     * X坐标
     */
    @ExcelProperty(value = "X坐标")
    private BigDecimal xCoordinate;

    /**
     * Y坐标
     */
    @ExcelProperty(value = "Y坐标")
    private BigDecimal yCoordinate;

    /**
     * 经度
     */
    @ExcelProperty(value = "经度")
    private BigDecimal longitude;

    /**
     * 纬度
     */
    @ExcelProperty(value = "纬度")
    private BigDecimal latitude;

    /**
     * 空间范围WKT
     */
    @ExcelProperty(value = "空间范围WKT")
    private String wkt;

    /**
     * 长
     */
    @ExcelProperty(value = "长")
    private BigDecimal lengthM;

    /**
     * 宽
     */
    @ExcelProperty(value = "宽")
    private BigDecimal widthM;

    /**
     * 高
     */
    @ExcelProperty(value = "高")
    private BigDecimal heightM;

    /**
     * 面积
     */
    @ExcelProperty(value = "面积")
    private BigDecimal areaSqm;

    /**
     * 体积
     */
    @ExcelProperty(value = "体积")
    private BigDecimal volumeCbm;

    /**
     * 规模等级
     */
    @ExcelProperty(value = "规模等级")
    private String scaleGrade;

    /**
     * 管理层级
     */
    @ExcelProperty(value = "管理层级")
    private String managementLevel;

    /**
     * 威胁人口
     */
    @ExcelProperty(value = "威胁人口")
    private Integer threatenedPopulation;

    /**
     * 威胁财产
     */
    @ExcelProperty(value = "威胁财产")
    private Integer threatenedPropertyValue;

    /**
     * 险情等级
     */
    @ExcelProperty(value = "险情等级")
    private String riskGrade;

    /**
     * 曾经发生灾害时间
     */
    @ExcelProperty(value = "曾经发生灾害时间")
    private String disasterHistoryTime;

    /**
     * 地质环境条件
     */
    @ExcelProperty(value = "地质环境条件")
    private String geologicalEnvironment;

    /**
     * 变形特征及活动历史
     */
    @ExcelProperty(value = "变形特征及活动历史")
    private String deformationFeatures;

    /**
     * 稳定性分析
     */
    @ExcelProperty(value = "稳定性分析")
    private String stabilityAnalysis;

    /**
     * 稳定性状态
     */
    @ExcelProperty(value = "稳定性状态")
    private String stabilityStatus;

    /**
     * 稳定性趋势
     */
    @ExcelProperty(value = "稳定性趋势")
    private String stabilityTrend;

    /**
     * 引发因数
     */
    @ExcelProperty(value = "引发因数")
    private String triggerFactors;

    /**
     * 潜在危害
     */
    @ExcelProperty(value = "潜在危害")
    private String potentialHazards;

    /**
     * 临灾状态预测
     */
    @ExcelProperty(value = "临灾状态预测")
    private String preDisasterPrediction;

    /**
     * 监测方法
     */
    @ExcelProperty(value = "监测方法")
    private String monitoringMethod;

    /**
     * 监测人网格人员关联ID
     */
    @ExcelProperty(value = "监测人网格人员关联ID")
    private String monitoringPersonId;

    /**
     * 填表日期
     */
    @ExcelProperty(value = "填表日期")
    private Date reportDate;

    /**
     * 隐患点历史标识
     */
    @ExcelProperty(value = "隐患点历史标识")
    private String historySn;

    /**
     * 数据标识
     */
    @ExcelProperty(value = "数据标识")
    private Integer dataFlag;

    /**
     * 是否核销（0=未核销，1=已核销）
     */
    @ExcelProperty(value = "是否核销", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0==未核销，1=已核销")
    private Integer isCancelled;

    /**
     * 申请类别
     */
    @ExcelProperty(value = "申请类别")
    private String operationType;

    /**
     * 审核状态
     */
    @ExcelProperty(value = "审核状态")
    private String reviewStatus;

    /**
     * 创建人
     */
    @ExcelProperty(value = "创建人")
    private String createdBy;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createdTime;

    /**
     * 是否有调查数据（0=无，1=有）
     */
    @ExcelProperty(value = "是否有调查数据", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0==无，1=有")
    private Integer hasSurveyData;

    /**
     * 隐患点分类
     */
    @ExcelProperty(value = "隐患点分类")
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

    /**
     * 专业监测设备总台套（变形监测L1 + 物理场监测L2 设备合计）
     * 由编排层按 uniqueDisasterId 批量聚合后填充，PO 中无对应字段
     */
    private Long professionalDeviceTotal;

    /**
     * GNSS 设备台数（monitoring_type 含 L1_GP）
     * 由编排层按 uniqueDisasterId 批量聚合后填充，PO 中无对应字段
     */
    private Long gnssCount;

    /**
     * 裂缝计台数（monitoring_type 含 L1_LF）
     * 由编排层按 uniqueDisasterId 批量聚合后填充，PO 中无对应字段
     */
    private Long crackMeterCount;
}
