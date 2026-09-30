package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistListHistory;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 任务提交历史记录视图对象 dz_task_dist_list_history
 *
 * @author kongweiguang
 * @date 2026-06-26
 */
@Data
@AutoMapper(target = DzTaskDistListHistory.class)
public class DzTaskDistListHistoryVo implements Serializable {

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
     * 任务提交文字记录
     */
    private String textRecord;

    /**
     * 兼容历史/feedbacks 响应中的 remark 字段。
     */
    private String remark;

    /**
     * 本次提交目标状态
     */
    private Integer submitStatus;

    /**
     * 任务提交时的现场照片OSS ID，多个以英文逗号分隔
     */
    private String scenePhoto;

    /**
     * 本次提交对应的打卡坐标点
     */
    private String checkCenter;

    /**
     * 打卡坐标生成的地点信息
     */
    private String checkCenterLocation;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;

    /**
     * 提交人id
     */
    private Long userId;

    /**
     * 提交人名称
     */
    private String userName;
}
