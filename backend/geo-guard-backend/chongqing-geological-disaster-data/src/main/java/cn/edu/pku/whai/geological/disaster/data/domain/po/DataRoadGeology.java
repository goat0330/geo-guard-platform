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
 * 道路地质信息对象 data_road_geology
 *
 * @author zhuzc
 * @date 2026-06-04
 */
@Data
@TableName("data_road_geology")
public class DataRoadGeology implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id")
    private String id;

    /** 道路名称 */
    @TableField("road_name")
    private String roadName;

    /** 几何WKT（TEXT 冗余字段，与 geom 同步） */
    @TableField("wkt")
    private String wkt;

    /** 几何对象（PostGIS geometry，4490，建议优先使用，建 GIST 索引） */
    @TableField(typeHandler = JtsGeometryWktTypeHandler.class)
    private String geom;
}
