/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("dz_def_resp_alarm_history")
public class DzDefRespAlarmHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /**
     * 警报主键id
     */
    private Long alarmId;

    /**
     * 触发的防御响应主键id
     */
    private Long defId;

    /**
     * 警报编号
     */
    private String alarmCode;

    /**
     * 触发时间
     */
    private Date triggerTime;

    /**
     * 触发的防御响应等级
     */
    private Integer defRespLevel;

    /**
     * 警报等级
     */
    private Integer alarmLevel;

    /**
     * 警报来源
     */
    private String alarmSource;

    /**
     * 警报来源类型（1:人工补录 2:解析报告）
     */
    private Integer alarmSourceType;

    /**
     * 创建时间
     */
    private Date createDate;
}
