/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSmsSendStat;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 短信发送单条记录视图对象 dz_sms_send_stat
 *
 * @author system
 * @date 2026-04-29
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DzSmsSendStat.class)
public class DzSmsSendStatVo implements Serializable {

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
     * 业务类型/来源类型
     */
    @ExcelProperty(value = "业务类型")
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
     * 接收人姓名
     */
    @ExcelProperty(value = "接收人姓名")
    private String receiverName;

    /**
     * 接收人手机号
     */
    @ExcelProperty(value = "接收人手机号")
    private String receiverPhone;

    /**
     * 实际发送短信内容
     */
    @ExcelProperty(value = "短信内容")
    private String smsContent;

    /**
     * 发送状态
     */
    @ExcelProperty(value = "发送状态")
    private String sendStatus;

    /**
     * 错误信息
     */
    @ExcelProperty(value = "错误信息")
    private String errorMsg;

    /**
     * 实际发送时间
     */
    @ExcelProperty(value = "发送时间")
    private Date sendTime;

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
