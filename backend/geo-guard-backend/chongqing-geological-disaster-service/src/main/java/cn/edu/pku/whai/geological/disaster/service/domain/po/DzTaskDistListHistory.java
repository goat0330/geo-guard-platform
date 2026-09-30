package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 任务提交历史记录表 dz_task_dist_list_history
 *
 * @author kongweiguang
 * @date 2026-06-26
 */
@Data
@TableName("dz_task_dist_list_history")
public class DzTaskDistListHistory implements Serializable {

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
     * 任务提交文字记录
     */
    @TableField("text_record")
    private String textRecord;

    /**
     * 本次提交目标状态
     */
    @TableField("submit_status")
    private Integer submitStatus;

    /**
     * 任务提交时的现场照片OSS ID，多个以英文逗号分隔
     */
    @TableField("scene_photo")
    private String scenePhoto;

    /**
     * 本次提交对应的打卡坐标点
     */
    @TableField("check_center")
    private String checkCenter;

    /**
     * 打卡坐标生成的地点信息
     */
    @TableField("check_center_location")
    private String checkCenterLocation;

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
     * 提交人id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 提交人名称（数据库无此列）
     */
    @TableField(exist = false)
    private String userName;
}
