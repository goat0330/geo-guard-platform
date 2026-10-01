/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("data_alarm")
public class DataAlarm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 编号
     */
    private String code;

    /**
     * 会话id
     */
    private String sessionId;

    /**
     * 所属市
     */
    private String city;

    /**
     * 预警等级与乡镇映射JSON
     */
    private String levelStreetsJson;

    /**
     * 预警发布日期
     */
    @TableField("\"time\"")
    private Date time;

    /**
     * 报告发布时间
     */
    private Date publishDate;

    /**
     * 预警发布有效开始时间
     */
    private Date validStartDate;

    /**
     * 预警发布有效结束时间
     */
    private Date validEndDate;

    /**
     * 状态（0:未触发 1:已触发 2:已过期 3:已替换）
     */
    private Integer status;

    /**
     * 待触发状态（0:无 1:待触发 2:已触发）
     */
    private Integer pendingStatus;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;

    /**
     * 消息来源
     */
    private String source;

    /**
     * 警报来源类型（1:人工补录 2:解析报告）
     */
    private Integer sourceType;

    /**
     * 原始消息
     */
    private String message;

    /**
     * 关键提示
     */
    private String keyTips;
}
