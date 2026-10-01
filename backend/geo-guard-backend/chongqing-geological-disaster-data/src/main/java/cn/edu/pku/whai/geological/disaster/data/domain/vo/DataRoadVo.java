/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.Road;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 道路信息视图对象
 *
 * @author system
 * @date 2026-05-15
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = Road.class)
public class DataRoadVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
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
