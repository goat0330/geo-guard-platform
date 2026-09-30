/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import java.util.Date;

/**
 * 第三方预警同步上下文。
 */
public record ThirdPartyWarningSyncContext(
    String warningId,
    String deviceId,
    String clientId,
    String monitorPointId,
    String slopeUnitId,
    String monitoringTypes,
    Integer warningLevel,
    Date warningTime,
    boolean newWarning
) {
}
