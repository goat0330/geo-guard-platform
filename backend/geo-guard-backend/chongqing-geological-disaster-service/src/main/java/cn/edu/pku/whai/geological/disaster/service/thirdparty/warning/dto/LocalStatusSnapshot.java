/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto;

import java.sql.Timestamp;
public record LocalStatusSnapshot(String deviceId, String clientId, Integer statusCode, String statusName, Timestamp lastOnlineTime, Timestamp sourceUpdateTime) {}
