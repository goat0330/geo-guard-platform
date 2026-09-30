/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class TaskDistPushBo extends DzTaskDistListBo {

    /**
     * 兼容旧推送接口的 taskId，内部会映射到列表查询的 id 条件
     */
    private Long taskId;

    /**
     * 批量任务ID集合
     */
    private List<Long> taskIds;
}
