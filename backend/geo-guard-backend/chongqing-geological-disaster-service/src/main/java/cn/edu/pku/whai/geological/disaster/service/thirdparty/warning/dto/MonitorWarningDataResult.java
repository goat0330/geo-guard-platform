/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto;

import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.MonitorWarningRecordResp;
import lombok.Getter;

import java.util.List;

@Getter
public class MonitorWarningDataResult {
    private final List<MonitorWarningRecordResp> records;
    private final Integer total;

    public MonitorWarningDataResult(List<MonitorWarningRecordResp> records, Integer total) {
        this.records = records;
        this.total = total;
    }
}
