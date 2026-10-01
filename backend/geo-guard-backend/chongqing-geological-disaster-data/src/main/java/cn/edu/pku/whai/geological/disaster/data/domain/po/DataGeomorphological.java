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
 * 地貌信息对象 data_geomorphological
 *
 * @author zhuzc
 * @date 2026-06-11
 */
@Data
@TableName("data_geomorphological")
public class DataGeomorphological implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 几何WKT（TEXT 冗余字段，与 geom 同步）
     */
    @TableField("wkt")
    private String wkt;

    /**
     * 地貌类型
     */
    @TableField("type")
    private String type;

    /**
     * 地貌特征
     */
    @TableField("feature")
    private String feature;

    /**
     * 几何对象（PostGIS geometry，4490，建 GIST 索引）
     */
    @TableField(typeHandler = JtsGeometryWktTypeHandler.class)
    private String geom;
}
