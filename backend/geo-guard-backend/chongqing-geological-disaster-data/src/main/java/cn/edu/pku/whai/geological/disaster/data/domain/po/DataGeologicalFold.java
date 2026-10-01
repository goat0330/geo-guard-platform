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
 * 地质褶皱对象 data_geological_fold
 *
 * @author zhuzc
 * @date 2026-06-17
 */
@Data
@TableName("data_geological_fold")
public class DataGeologicalFold implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id")
    private Long id;

    /** 原始MULTILINESTRING WKT文本 */
    private String wkt;

    /** 褶皱名称 */
    private String name;

    /** 褶皱长度(km) */
    private String length;

    /** 褶皱宽度(km) */
    private String width;

    /** 褶皱简要描述 */
    @TableField("fold_summary")
    private String foldSummary;

    /** 几何对象（PostGIS geometry MultiLineString，4490，建 GIST 索引） */
    @TableField(typeHandler = JtsGeometryWktTypeHandler.class)
    private String geom;
}
