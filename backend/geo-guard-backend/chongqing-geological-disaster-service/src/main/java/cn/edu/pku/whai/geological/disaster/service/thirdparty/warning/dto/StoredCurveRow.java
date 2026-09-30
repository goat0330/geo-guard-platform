/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto;

import java.sql.Timestamp;
public record StoredCurveRow(Timestamp sourceUpdateTime, String columnsJson, String pointValuesJson) {}
