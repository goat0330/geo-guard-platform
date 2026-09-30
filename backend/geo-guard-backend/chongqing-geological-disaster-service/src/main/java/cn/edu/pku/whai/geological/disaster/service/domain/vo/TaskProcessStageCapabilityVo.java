/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务流程链路阶段能力字段展示对象。
 */
@Data
public class TaskProcessStageCapabilityVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 阶段分类编码。
     */
    private Integer stageType;

    /**
     * 阶段分类展示名。
     */
    private String stageTypeLabel;

    /**
     * 当前阶段已经调用的技能字段。
     */
    private List<String> currentCapabilities = new ArrayList<>();

    /**
     * 当前阶段下一步需要调用的技能字段。
     */
    private List<String> nextCapabilities = new ArrayList<>();
}
