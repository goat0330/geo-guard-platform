/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistListRemark;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 任务备注视图对象 dz_task_dist_list_remark
 */
@Data
@AutoMapper(target = DzTaskDistListRemark.class)
public class DzTaskDistListRemarkVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 任务表id
     */
    private Long taskId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;

    /**
     * 备注人id
     */
    private Long userId;

    /**
     * 备注人名称
     */
    private String userName;
}
