/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 短信发送结果汇总展示对象。
 */
@Data
@Builder
@ExcelIgnoreUnannotated
public class SmsSendSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 短信目标总数 */
    @ExcelProperty(value = "短信目标总数")
    private int totalCount;

    /** 实际发送成功数 */
    @ExcelProperty(value = "实际发送成功数")
    private int successCount;

    /** 实际发送失败数 */
    @ExcelProperty(value = "实际发送失败数")
    private int failCount;

    /** 被动态配置跳过数 */
    @ExcelProperty(value = "配置跳过数")
    private int skipCount;
}
