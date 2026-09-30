/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import cn.edu.pku.whai.geological.disaster.data.typehandler.PgJacksonTypeHandler;
import cn.edu.pku.whai.geological.disaster.service.consts.AutoModeExecutionActions;
import cn.edu.pku.whai.geological.disaster.service.consts.AutoModeExecutionStatus;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 自动模式单次执行留痕对象 dz_auto_mode_execution_trace。
 */
@Data
@TableName(value = "dz_auto_mode_execution_trace", autoResultMap = true)
public class DzAutoModeExecutionTrace implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 动作类型：1处置自动推进 2未推送任务自动推送 3日常巡逻自动推送 4自动催办 5报灾自动处理 6报灾任务推送 7风险预测推送 8日常巡逻生成
     */
    private Integer actionType;

    /**
     * 动作展示名称。
     */
    private String actionName;

    /**
     * 业务类型：0未知 1任务 2报灾/报告 3处置管理 4防御响应 5预警信息 6监测预警。
     */
    private Integer bizType;

    /**
     * 业务主键。
     */
    private Long bizId;

    /**
     * 关联任务派发 ID。
     */
    private Long taskId;

    /**
     * 是否批量动作。
     */
    private Boolean batchFlag;

    /**
     * 本次成功执行数量。
     */
    private Integer executeCount;

    /**
     * 执行状态：1执行中 2执行成功 3执行失败。
     */
    private Integer status;

    /**
     * 执行开始时间。
     */
    private Date startedAt;

    /**
     * 执行结束时间。
     */
    private Date endedAt;

    /**
     * 失败原因。
     */
    private String failureReason;

    /**
     * 执行补充信息。
     */
    @TableField(typeHandler = PgJacksonTypeHandler.class)
    private Map<String, Object> detailJson;

    /**
     * 创建时间。
     */
    private Date createDate;

    /**
     * 更新时间。
     */
    private Date updateDate;

    public void setActionType(Integer actionType) {
        this.actionType = actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = AutoModeExecutionActions.resolveCode(actionType);
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setStatus(String status) {
        this.status = AutoModeExecutionStatus.resolveCode(status);
    }
}
