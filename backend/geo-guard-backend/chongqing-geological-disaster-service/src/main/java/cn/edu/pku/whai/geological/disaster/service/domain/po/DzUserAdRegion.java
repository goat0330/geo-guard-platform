/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户行政区划关联对象 dz_user_ad_region
 */
@Data
@TableName("dz_user_ad_region")
public class DzUserAdRegion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId
    private Long userId;

    /**
     * 行政区划ID
     */
    private String adRegionId;

    /**
     * 行政区划名称
     */
    private String adRegionName;

    /**
     * 行政区划层级:1省2市3县4乡镇5村
     */
    private Integer adRegionLevel;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 删除标志:0存在1删除
     */
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
