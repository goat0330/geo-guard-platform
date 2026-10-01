/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;


import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import cn.edu.pku.whai.geological.disaster.data.domain.po.RiskZone;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;


/**
 * 风险区视图对象 data_risk_zone
 *
 * @author lizheng
 * @date 2026-01-10
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = RiskZone.class)
public class RiskZoneVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 唯一标识符
     */
    @ExcelProperty(value = "唯一标识符")
    private String id;

    /**
     * 是否属于588范围
     */
    @ExcelProperty(value = "是否属于588范围")
    private Integer pilotArea1;

    /**
     * 是否属于46范围
     */
    @ExcelProperty(value = "是否属于46范围")
    private Integer pilotArea2;

    /**
     * 所属省份
     */
    @ExcelProperty(value = "所属省份")
    private String province;

    /**
     * 所属地级市
     */
    @ExcelProperty(value = "所属地级市")
    private String city;

    /**
     * 所属区/县
     */
    @ExcelProperty(value = "所属区/县")
    private String county;

    /**
     * 所属乡镇/街道
     */
    @ExcelProperty(value = "所属乡镇/街道")
    private String street;

    /**
     * 所属行政村
     */
    @ExcelProperty(value = "所属行政村")
    private String village;

    /**
     * 所属社区（若适用）
     */
    @ExcelProperty(value = "所属社区", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "若=适用")
    private String community;

    /**
     * 中心点坐标
     */
    @ExcelProperty(value = "中心点坐标")
    private String center;

    /**
     * 边界多边形
     */
    @ExcelProperty(value = "边界多边形")
    private String wkt;

    /**
     * $column.columnComment
     */
    @ExcelProperty(value = "${comment}", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "$column.readConverterExp()")
    private BigDecimal area;

    /**
     * 动态风险值
     */
    @ExcelProperty(value = "动态风险值")
    private Integer dynamicRiskValue;

    /**
     * 省编码
     */
    @ExcelProperty(value = "省编码")
    private String provinceCode;

    /**
     * 市编码
     */
    @ExcelProperty(value = "市编码")
    private String cityCode;

    /**
     * 区县编码
     */
    @ExcelProperty(value = "区县编码")
    private String countyCode;

    /**
     * 街道/乡镇编码
     */
    @ExcelProperty(value = "街道/乡镇编码")
    private String streetCode;

    /**
     * 村/社区编码
     */
    @ExcelProperty(value = "村/社区编码")
    private String villageCode;

    private Integer dynamicRiskLevel;
}
