/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 报灾处理结果。
 */
@Data
public class DzReportDisasterHandleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务派发对象名称
     */
    private String dispatchTargetName;

    /**
     * 所属区/县/县级市全称
     */
    private String county;

    /**
     * 所属街道全称
     */
    private String street;

    /**
     * 所属社区全称
     */
    private String village;
}
