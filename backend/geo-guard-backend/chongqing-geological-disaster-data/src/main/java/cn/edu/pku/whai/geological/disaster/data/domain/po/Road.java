/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 道路信息表 data_road
 *
 * @author system
 * @date 2026-05-15
 */
@Data
@TableName("public.data_road")
public class Road implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private String id;

    /**
     * 道路名称
     */
    private String roadName;

    /**
     * 道路等级
     */
    private String roadLevel;

    /**
     * 道路WKT
     */
    private String wkt;

    /**
     * 斜坡单元ID
     */
    private String slopeUnitId;
}
