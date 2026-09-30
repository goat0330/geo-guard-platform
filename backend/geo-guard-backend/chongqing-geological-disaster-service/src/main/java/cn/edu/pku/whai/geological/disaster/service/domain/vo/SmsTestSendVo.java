/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 短信发送测试结果
 *
 * @author whai
 */
@Data
public class SmsTestSendVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 目标手机号
     */
    @ExcelProperty(value = "目标手机号")
    private String phone;

    /**
     * 短信正文
     */
    @ExcelProperty(value = "短信正文")
    private String content;

    /**
     * 短信平台总开关和测试短信开关是否均开启
     */
    @ExcelProperty(value = "短信发送已启用")
    private boolean smsEnabled;

    /**
     * 是否发送成功
     */
    @ExcelProperty(value = "发送成功")
    private boolean success;

    /**
     * 结果说明
     */
    @ExcelProperty(value = "结果说明")
    private String message;
}
