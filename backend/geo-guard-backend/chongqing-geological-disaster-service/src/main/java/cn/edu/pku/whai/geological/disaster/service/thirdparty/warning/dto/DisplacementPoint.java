/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto;

import java.time.LocalDateTime;
public record DisplacementPoint(LocalDateTime time, Long timestampMillis, Double x, Double y, Double z) {}
