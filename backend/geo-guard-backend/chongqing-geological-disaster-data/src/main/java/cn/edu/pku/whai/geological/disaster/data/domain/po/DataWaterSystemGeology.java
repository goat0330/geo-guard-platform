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
 * 水系地质信息对象 data_water_system_geology
 *
 * @author zhuzc
 * @date 2026-06-04
 */
@Data
@TableName("data_water_system_geology")
public class DataWaterSystemGeology implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id")
    private String id;

    /** 水系名称 */
    @TableField("water_system_name")
    private String waterSystemName;

    /** 几何WKT（TEXT 冗余字段，与 geom 同步） */
    @TableField("wkt")
    private String wkt;

    /** 几何对象（PostGIS geometry，4490，建议优先使用，建 GIST 索引） */
    @TableField(typeHandler = JtsGeometryWktTypeHandler.class)
    private String geom;
}
