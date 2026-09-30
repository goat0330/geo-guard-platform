/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSmsSendBatch;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 短信发送批次汇总业务对象 dz_sms_send_batch
 *
 * @author system
 * @date 2026-05-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzSmsSendBatch.class, reverseConvertGenerate = false)
public class DzSmsSendBatchBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "id不能为空", groups = {EditGroup.class})
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
