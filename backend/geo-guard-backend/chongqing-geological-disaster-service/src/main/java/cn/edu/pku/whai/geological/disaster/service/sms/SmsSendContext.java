/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sms;

import lombok.Builder;
import lombok.Data;

/**
 * 短信发送上下文，传入后由 {@link SmsSendService} 自动写入发送记录。
 * <p>单发写入 {@code dz_sms_send_stat}；群发 additionally 写入 {@code dz_sms_send_batch} 汇总。</p>
 */
@Data
@Builder
public class SmsSendContext {

    /**
     * 短信发送场景，用于动态开关判断和发送审计。
     */
    private SmsScene scene;

    /**
     * 任务来源类型，仅任务推送场景使用：0 手动添加、1 系统评估、2 群众上报、
     * 3 防御响应、4 应急处置、5 监测预警、6 技术协查。
     */
    private Integer taskSourceType;

    /**
     * 业务对象类型
     */
    private Integer bizType;

    /**
     * 业务对象 ID
     */
    private Long bizId;

    /**
     * 短信类型
     */
    private String smsType;

    /**
     * 接收人姓名（单发或群发共用；群发时各号码可共用同一姓名）
     */
    private String receiverName;

    /**
     * 批次 ID（群发时未传则自动生成）
     */
    private String batchId;

    /**
     * 发送对象类型（仅群发批次汇总使用）
     */
    private String targetType;

}
