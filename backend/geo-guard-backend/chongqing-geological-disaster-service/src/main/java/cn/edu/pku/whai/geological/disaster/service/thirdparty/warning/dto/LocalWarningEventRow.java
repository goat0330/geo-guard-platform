/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto;

import java.sql.Timestamp;
public record LocalWarningEventRow(String warningId, String deviceId, String clientId, String monitorPointId, String monitorPointName, String warningDeviceName, Integer warningLevel, Timestamp warningTime, Integer disposalStatus, String disposalType, Timestamp disposalTime, String disposalPerson, Integer validWarning, Timestamp sourceUpdateTime) {}
