/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 任务备注表 dz_task_dist_list_remark
 */
@Data
@TableName("dz_task_dist_list_remark")
public class DzTaskDistListRemark implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 任务表id
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField("create_date")
    private Date createDate;

    /**
     * 更新时间
     */
    @TableField("update_date")
    private Date updateDate;

    /**
     * 备注人id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 备注人名称（数据库无此列）
     */
    @TableField(exist = false)
    private String userName;
}
