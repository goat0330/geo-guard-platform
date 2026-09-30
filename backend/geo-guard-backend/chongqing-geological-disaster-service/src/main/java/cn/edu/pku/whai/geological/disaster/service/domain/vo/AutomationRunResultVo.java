/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 自动化运营单轮执行结果。
 */
@Data
public class AutomationRunResultVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int pushedTaskCount;

    private int remindedTaskCount;

    private int createdPatrolTaskCount;

    private int handledReportCount;

    private int pushedPredictionCount;

    private int noticeCount;
}
