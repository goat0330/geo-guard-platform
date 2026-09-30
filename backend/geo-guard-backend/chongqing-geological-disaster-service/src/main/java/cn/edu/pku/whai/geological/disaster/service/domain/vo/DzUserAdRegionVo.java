/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzUserAdRegion;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户行政区划关联视图对象 dz_user_ad_region
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DzUserAdRegion.class)
public class DzUserAdRegionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @ExcelProperty(value = "用户ID")
    private Long userId;

    /**
     * 行政区划ID
     */
    @ExcelProperty(value = "行政区划ID")
    private String adRegionId;

    /**
     * 行政区划名称
     */
    @ExcelProperty(value = "行政区划名称")
    private String adRegionName;

    /**
     * 行政区划层级:1省2市3县4乡镇5村
     */
    @ExcelProperty(value = "行政区划层级")
    private Integer adRegionLevel;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;

    /**
     * 删除标志:0存在1删除
     */
    @ExcelProperty(value = "删除标志")
    private String delFlag;
}
