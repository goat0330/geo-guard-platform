/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto;

import java.util.Map;
public record LocalStatusLookup(Map<String, LocalStatusSnapshot> byDeviceId, Map<String, LocalStatusSnapshot> byClientId) {}
