/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzUserAdRegion;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 用户行政区划关联业务对象 dz_user_ad_region
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzUserAdRegion.class, reverseConvertGenerate = false)
public class DzUserAdRegionBo extends BaseEntity {

    /**
     * 用户ID
     */
//    @NotNull(message = "用户ID不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long userId;

    /**
     * 用户ID列表
     */
    private List<Long> userIds;

    /**
     * 行政区划ID
     */
    @NotBlank(message = "行政区划ID不能为空", groups = {AddGroup.class, EditGroup.class})
    private String adRegionId;

    /**
     * 行政区划兼容查询ID列表，包含当前查询区划及其上级区划。
     */
    private List<String> compatibleAdRegionIds;

    /**
     * 行政区划名称
     */
//    @NotBlank(message = "行政区划名称不能为空", groups = {EditGroup.class})
    private String adRegionName;

    /**
     * 行政区划层级:1省2市3县4乡镇5村
     */
//    @NotNull(message = "行政区划层级不能为空", groups = {EditGroup.class})
    private Integer adRegionLevel;

    /**
     * 统计返回条数限制，用于首页图表取前几个区域。
     */
    private Integer limit;

}
