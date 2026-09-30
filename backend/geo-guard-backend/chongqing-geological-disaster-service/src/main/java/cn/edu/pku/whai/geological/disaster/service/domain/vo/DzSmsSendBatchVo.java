/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSmsSendBatch;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 短信发送批次汇总视图对象 dz_sms_send_batch
 *
 * @author system
 * @date 2026-05-18
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DzSmsSendBatch.class)
public class DzSmsSendBatchVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 批次ID
     */
    @ExcelProperty(value = "批次ID")
    private String batchId;

    /**
     * 业务对象类型
     */
    @ExcelProperty(value = "业务对象类型")
    private Integer bizType;

    /**
     * 业务对象ID
     */
    @ExcelProperty(value = "业务对象ID")
    private Long bizId;

    /**
     * 短信类型
     */
    @ExcelProperty(value = "短信类型")
    private String smsType;

    /**
     * 短信发送场景编码
     */
    @ExcelProperty(value = "短信发送场景")
    private String sceneCode;

    /**
     * 任务来源类型：0 手动添加、1 系统评估、2 群众上报、3 防御响应、4 应急处置、5 监测预警、6 技术协查
     */
    @ExcelProperty(value = "任务来源类型")
    private Integer taskSourceType;

    /**
     * 发送对象类型
     */
    @ExcelProperty(value = "发送对象类型")
    private String targetType;

    /**
     * 短信正文快照
     */
    @ExcelProperty(value = "短信正文快照")
    private String contentSnapshot;

    /**
     * 批次发送状态
     */
    @ExcelProperty(value = "批次发送状态")
    private String sendStatus;

    /**
     * 发送总数
     */
    @ExcelProperty(value = "发送总数")
    private Integer totalCount;

    /**
     * 发送成功数
     */
    @ExcelProperty(value = "发送成功数")
    private Integer successCount;

    /**
     * 发送失败数
     */
    @ExcelProperty(value = "发送失败数")
    private Integer failCount;

    /**
     * 按配置跳过发送数
     */
    @ExcelProperty(value = "跳过发送数")
    private Integer skipCount;

    /**
     * 发起时间
     */
    @ExcelProperty(value = "发起时间")
    private Date requestTime;

    /**
     * 完成时间
     */
    @ExcelProperty(value = "完成时间")
    private Date finishTime;

    /**
     * 批次错误信息
     */
    @ExcelProperty(value = "批次错误信息")
    private String errorMsg;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createDate;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateDate;
}
