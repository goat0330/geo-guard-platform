/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl.helper;

import java.util.Objects;

public class TaskSmsGroupKey {
    public final Long userId;
    public final Integer sourceType;
    public final Integer planType;
    public final String taskType;
    public final String taskSubType;

    public TaskSmsGroupKey(Long userId, Integer sourceType) {
        this(userId, sourceType, null, null, null);
    }

    public TaskSmsGroupKey(Long userId, Integer sourceType, Integer planType, String taskType, String taskSubType) {
        this.userId = userId;
        this.sourceType = sourceType;
        this.planType = planType;
        this.taskType = normalize(taskType);
        this.taskSubType = normalize(taskSubType);
    }

    public Integer sourceType() { return sourceType; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TaskSmsGroupKey other)) {
            return false;
        }
        return Objects.equals(userId, other.userId)
            && Objects.equals(sourceType, other.sourceType)
            && Objects.equals(planType, other.planType)
            && Objects.equals(taskType, other.taskType)
            && Objects.equals(taskSubType, other.taskSubType);
    }

    @Override
    public int hashCode() { return Objects.hash(userId, sourceType, planType, taskType, taskSubType); }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
