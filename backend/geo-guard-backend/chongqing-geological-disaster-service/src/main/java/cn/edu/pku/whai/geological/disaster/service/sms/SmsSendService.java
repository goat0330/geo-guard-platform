/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sms;

import cn.edu.pku.whai.geological.disaster.service.domain.vo.EvacuationSmsResult;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.SmsTestSendVo;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * 短信发送能力接口（预留），后续接入真实短信通道时实现
 *
 * @author whai
 */
public interface SmsSendService {

    /**
     * 点对点批量短信条目。
     *
     * @param mobile  目标手机号
     * @param content 短信正文
     */
    record BatchSendItem(String mobile, String content) {
    }

    /**
     * 向一批号码发送同一条短信正文
     *
     * @param content 短信正文
     * @param phones  目标号码列表（已去重、已过滤空号）
     * @return 该次群发结果（成功数、失败数、失败号码等）
     */
    EvacuationSmsResult send(String content, List<String> phones);

    /**
     * 向一批号码发送同一条短信正文，并按上下文自动写入 dz_sms_send_batch 与 dz_sms_send_stat。
     */
    EvacuationSmsResult send(String content, List<String> phones, SmsSendContext context);

    /**
     * 点对点批量发送，每个号码可发送不同正文。
     */
    EvacuationSmsResult batchSend(List<BatchSendItem> items, SmsSendContext context);

    Boolean send(String content, String phone);

    /**
     * 向单个号码发送短信，并按上下文自动写入 dz_sms_send_stat。
     */
    Boolean send(String content, String phone, SmsSendContext context);

    /**
     * 测试短信发送，返回真实发送结果
     *
     * @param content 短信正文
     * @param phone   目标手机号
     * @return 发送测试结果
     */
    SmsTestSendVo testSend(String content, String phone);

    /**
     * 查询短信平台状态报告。
     */
    JsonNode queryReport();
}
