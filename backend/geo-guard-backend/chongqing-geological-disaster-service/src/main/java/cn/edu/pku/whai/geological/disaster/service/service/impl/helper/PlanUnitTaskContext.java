/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl.helper;

import java.util.List;
import java.util.Map;

public record PlanUnitTaskContext(Map<Long, List<String>> unitIdsByPlanId, Map<Long, MonitorFrequencyParams> frequencyByPlanId, Map<Long, Long> handleIdByPlanId) {}
