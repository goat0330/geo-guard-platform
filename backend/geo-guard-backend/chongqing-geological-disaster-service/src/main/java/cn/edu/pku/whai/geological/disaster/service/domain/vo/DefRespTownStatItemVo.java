/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 乡镇级防御响应统计项。
 */
@Data
public class DefRespTownStatItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 响应等级。
     */
    private Integer level;

    /**
     * 响应等级名称。
     */
    private String levelName;

    /**
     * 当前统计范围内全部流转中乡镇，按名称去重后拼接。
     */
    private String allStreets;

    /**
     * 当前统计范围内最新流转中响应状态。
     */
    private Integer status;

    /**
     * 当前统计范围内最新流转中执行状态。
     */
    private Integer executeStatus;

    /**
     * 当前统计范围内最新流转中响应启动时间。
     */
    private Date startTime;

    /**
     * 乡镇数量。
     */
    private Long townCount;

    /**
     * 面积，单位：平方千米。
     */
    private BigDecimal areaSquareKilometer;

    /**
     * 人口数量，单位：万人。
     */
    private BigDecimal populationTenThousand;

    /**
     * 灾害点数量。
     */
    private Long disasterPointCount;
}
