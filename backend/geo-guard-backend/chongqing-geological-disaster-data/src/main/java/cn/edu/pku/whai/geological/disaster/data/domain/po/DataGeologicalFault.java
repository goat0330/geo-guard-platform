/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import cn.edu.pku.whai.geological.disaster.data.typehandler.JtsGeometryWktTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 地质断层对象 data_geological_fault
 *
 * @author zhuzc
 * @date 2026-06-17
 */
@Data
@TableName("data_geological_fault")
public class DataGeologicalFault implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id")
    private Long id;

    /** 原始WKT内容 */
    private String wkt;

    /** 断层名称 */
    private String name;

    /** 走向 */
    private String strike;

    /** 长度(km) */
    private String length;

    /** 宽度(m) */
    private String width;

    /** 断层特征 */
    private String feature;

    /** 断层性质 */
    private String property;

    /** 倾向 */
    @TableField("dip_direction")
    private String dipDirection;

    /** 倾角 */
    @TableField("dip_angle")
    private String dipAngle;

    /** 几何对象（PostGIS geometry MultiLineString，4490，建 GIST 索引） */
    @TableField(typeHandler = JtsGeometryWktTypeHandler.class)
    private String geom;
}
