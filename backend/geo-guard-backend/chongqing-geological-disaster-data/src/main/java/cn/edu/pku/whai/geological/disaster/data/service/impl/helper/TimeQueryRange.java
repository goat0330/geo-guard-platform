/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl.helper;

import java.time.LocalDateTime;

/**
 * 时间查询范围（内部 record 提取）。
 */
public record TimeQueryRange(LocalDateTime startTime, LocalDateTime endTime, boolean dayPrecision) {}
