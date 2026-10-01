/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import cn.edu.pku.whai.geological.disaster.data.typehandler.JtsGeometryWktTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 斜坡单元对象 data_slope_unit
 *
 * @author kongweiguang
 * @date 2026-01-05
 */
@Data
@TableName("public.data_slope_unit")
public class SlopeUnit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 唯一标识符
     */
    @TableId(value = "id")
    private String id;

    /**
     * 斜坡单元名称
     */
    private String name;

    /**
     * 是否属于588范围
     */
    @TableField("pilot_area_1")
    private Integer pilotArea1;

    /**
     * 是否属于46范围
     */
    @TableField("pilot_area_2")
    private Integer pilotArea2;

    /**
     * 是否属于城区低风险范围
     */
    private Integer urbanArea;

    /**
     * 所属省份
     */
    private String province;

    /**
     * 所属地级市
     */
    private String city;

    /**
     * 所属区/县
     */
    private String county;

    /**
     * 所属乡镇/街道
     */
    private String street;

    /**
     * 所属行政村
     */
    private String village;

    /**
     * 所属社区（若适用）
     */
    private String community;

    /**
     * 中心点坐标
     */
    private String center;

    /**
     * 详细地址
     */
    private String detailedAddress;

    /**
     * 边界多边形（WKT 文本）
     */
    private String wkt;

    /**
     * 几何对象（PostGIS geometry，SRID 4326）
     */
    @TableField(typeHandler = JtsGeometryWktTypeHandler.class)
    private String geom;

    /**
     * $column.columnComment
     */
    private BigDecimal area;

    /**
     * 省编码
     */
    private String provinceCode;

    /**
     * 市编码
     */
    private String cityCode;

    /**
     * 区县编码
     */
    private String countyCode;

    /**
     * 街道/乡镇编码
     */
    private String streetCode;

    /**
     * 村/社区编码
     */
    private String villageCode;

}
