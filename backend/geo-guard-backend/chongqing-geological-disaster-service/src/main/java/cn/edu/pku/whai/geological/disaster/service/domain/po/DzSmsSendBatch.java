/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 短信发送批次汇总对象 dz_sms_send_batch
 *
 * @author system
 * @date 2026-05-18
 */
@Data
@TableName("dz_sms_send_batch")
public class DzSmsSendBatch implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务类型：推送任务
     */
    public static final int BIZ_TYPE_TASK_PUSH = 1;

    /**
     * 业务类型：撤离短信
     */
    public static final int BIZ_TYPE_EVACUATION_SMS = 2;

    /**
     * 业务类型：防御响应
     */
    public static final int BIZ_TYPE_DEF_RESP = 3;

    /**
     * 短信类型：群众撤离短信
     */
    public static final String SMS_TYPE_MASSES = "群众撤离";

    /**
     * 短信类型：负责人撤离短信
     */
    public static final String SMS_TYPE_PRINCIPAL = "负责人撤离";

    /**
     * 发送对象：群众
     */
    public static final String TARGET_TYPE_MASSES = "MASSES";

    /**
     * 发送对象：负责人
     */
    public static final String TARGET_TYPE_PRINCIPAL = "PRINCIPAL";

    /**
     * 批次状态：成功
     */
    public static final String SEND_STATUS_SUCCESS = "SUCCESS";

    /**
     * 批次状态：部分成功
     */
    public static final String SEND_STATUS_PART_SUCCESS = "PART_SUCCESS";

    /**
     * 批次状态：失败
     */
    public static final String SEND_STATUS_FAIL = "FAIL";

    /**
     * 批次状态：未发送
     */
    public static final String SEND_STATUS_INIT = "INIT";

    /**
     * 批次状态：按配置跳过发送
     */
    public static final String SEND_STATUS_SKIPPED = "SKIPPED";

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
     * 发送对象类型
     */
    private String targetType;

    /**
     * 短信正文快照
     */
    private String contentSnapshot;

    /**
     * 批次发送状态
     */
    private String sendStatus;

    /**
     * 发送总数
     */
    private Integer totalCount;

    /**
     * 发送成功数
     */
    private Integer successCount;

    /**
     * 发送失败数
     */
    private Integer failCount;

    /**
     * 按配置跳过发送数
     */
    private Integer skipCount;

    /**
     * 发起时间
     */
    private Date requestTime;

    /**
     * 完成时间
     */
    private Date finishTime;

    /**
     * 批次错误信息
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;
}
