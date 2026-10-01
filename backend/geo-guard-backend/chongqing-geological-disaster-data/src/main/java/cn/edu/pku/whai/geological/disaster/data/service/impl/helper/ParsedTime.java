/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl.helper;

import java.time.LocalDateTime;

/**
 * 解析后的时间（内部 record 提取）。
 */
public record ParsedTime(LocalDateTime time, boolean dayPrecision) {}
