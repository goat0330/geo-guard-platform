/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务流程链路能力字段展示对象。
 */
@Data
public class TaskProcessCapabilityVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程链路 id。
     */
    private String chainId;

    /**
     * 当前已经调用的技能字段。
     */
    private List<String> currentCapabilities = new ArrayList<>();

    /**
     * 下一步需要调用的技能字段。
     */
    private List<String> nextCapabilities = new ArrayList<>();

    /**
     * 按任务流程阶段汇总的技能字段。
     */
    private List<TaskProcessStageCapabilityVo> stageCapabilities = new ArrayList<>();
}
