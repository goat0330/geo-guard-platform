/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 短信发送单条记录对象 dz_sms_send_stat
 *
 * @author system
 * @date 2026-04-29
 */
@Data
@TableName("dz_sms_send_stat")
public class DzSmsSendStat implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 批次ID
     */
    private String batchId;

    /**
     * 业务对象类型
     */
    private Integer bizType;

    /**
     * 业务对象ID
     */
    private Long bizId;

    /**
     * 短信类型
     */
    private String smsType;

    /**
     * 短信发送场景编码
     */
    private String sceneCode;

    /**
     * 任务来源类型：0 手动添加、1 系统评估、2 群众上报、3 防御响应、4 应急处置、5 监测预警、6 技术协查
     */
    private Integer taskSourceType;

    /**
     * 接收人姓名
     */
    private String receiverName;

    /**
     * 接收人手机号
     */
    private String receiverPhone;

    /**
     * 实际发送短信内容
     */
    private String smsContent;

    /**
     * 发送状态
     */
    private String sendStatus;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 实际发送时间
     */
    private Date sendTime;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;
}
