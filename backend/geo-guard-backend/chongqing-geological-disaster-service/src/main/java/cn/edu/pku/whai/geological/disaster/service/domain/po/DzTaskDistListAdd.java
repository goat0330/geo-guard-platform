package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 任务上报信息表 dz_task_dist_list_add
 *
 * @author kongweiguang
 * @date 2026-04-15
 */
@Data
@TableName("dz_task_dist_list_add")
public class DzTaskDistListAdd implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务 id（联合主键）
     */
    private Long taskId;

    /**
     * 用户 id（联合主键）
     */
    private Long userId;

    /**
     * 上报内容
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 上报人id
     */
    private Long reportUserId;
}
