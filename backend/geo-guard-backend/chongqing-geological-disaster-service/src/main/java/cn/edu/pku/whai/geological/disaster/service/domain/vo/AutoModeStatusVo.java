/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 自动模式状态。
 */
@Data
@ExcelIgnoreUnannotated
public class AutoModeStatusVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 自动模式开关状态。
     */
    @ExcelProperty(value = "自动模式开关状态")
    private Boolean enabled;

    /**
     * 自动执行每一步之间的间隔秒数。
     */
    @ExcelProperty(value = "步骤间隔秒数")
    private Integer stepIntervalSeconds;

    /**
     * 自动执行人名称。
     */
    @ExcelProperty(value = "自动执行人名称")
    private String actorName;

    /**
     * 自动执行人用户 ID。
     */
    @ExcelProperty(value = "自动执行人用户ID")
    private Long actorUserId;

    /**
     * 最后更新人用户 ID。
     */
    @ExcelProperty(value = "最后更新人用户ID")
    private Long updatedBy;

    /**
     * 最后更新时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty(value = "最后更新时间")
    private Date updatedAt;

    /**
     * 最近一次开启时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty(value = "最近一次开启时间")
    private Date openedAt;

    /**
     * 最近一次关闭时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty(value = "最近一次关闭时间")
    private Date closedAt;

    /**
     * 自动化运营摘要信息。
     */
    @ExcelProperty(value = "自动化运营摘要")
    private Map<String, Object> automationSummary;
}
