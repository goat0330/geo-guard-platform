/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl.helper;

import lombok.Data;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

@Data
public class BatchUpdateTownLevelsExecutionResult {
    public final Set<Long> affectedTownDefIds;
    public final Map<Long, Boolean> countiesToReconcile;

    public BatchUpdateTownLevelsExecutionResult(Set<Long> affectedTownDefIds, Map<Long, Boolean> countiesToReconcile) {
        this.affectedTownDefIds = affectedTownDefIds;
        this.countiesToReconcile = countiesToReconcile;
    }

}
