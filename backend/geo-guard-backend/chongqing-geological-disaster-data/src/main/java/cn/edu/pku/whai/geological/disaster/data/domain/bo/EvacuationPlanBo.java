/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.data.domain.po.EvacuationPlan;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 撤离方案表查询/业务对象
 *
 * @author whai
 * @date 2026-02-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = EvacuationPlan.class, reverseConvertGenerate = false)
public class EvacuationPlanBo extends BaseEntity {

    /**
     * 主键
     */
    private Long id;

    /**
     * 方案编号
     */
    private Long handleId;

    /**
     * 撤离区域
     */
    private String evacuationArea;

    /**
     * 安置点
     */
    private String resettlementLocation;

    /**
     * 撤离方向
     */
    private String evacuationRoute;

    /**
     * 创建时间
     */
    private Date createTime;
}
