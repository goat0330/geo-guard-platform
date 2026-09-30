/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import cn.edu.pku.whai.geological.disaster.data.typehandler.PgJacksonTypeHandler;
import cn.edu.pku.whai.geological.disaster.service.consts.AutoModeExecutionActions;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 自动模式分钟汇总对象 dz_auto_mode_minute_summary。
 */
@Data
@TableName(value = "dz_auto_mode_minute_summary", autoResultMap = true)
public class DzAutoModeMinuteSummary implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 分钟窗口开始时间。
     */
    private Date windowStart;

    /**
     * 动作类型：1处置自动推进 2未推送任务自动推送 3日常巡逻自动推送 4自动催办 5报灾自动处理 6报灾任务推送 7风险预测推送 8日常巡逻生成
     */
    private Integer actionType;

    /**
     * 动作展示名称。
     */
    private String actionName;

    /**
     * 该分钟成功执行数量。
     */
    private Integer executeCount;

    /**
     * 汇总补充信息。
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
}
