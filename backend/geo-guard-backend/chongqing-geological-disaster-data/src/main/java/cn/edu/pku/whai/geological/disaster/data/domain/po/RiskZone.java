/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 风险区对象 data_risk_zone
 *
 * @author lizheng
 * @date 2026-01-10
 */
@Data
@TableName("data_risk_zone")
public class RiskZone implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 唯一标识符
     */
    @TableId(value = "id")
    private String id;

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
     * 边界多边形
     */
    private String wkt;

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

    private String slopeUnitId;

    private String oName;
}
