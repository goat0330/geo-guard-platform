/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.resp.warning;

import lombok.Data;

import java.util.List;

@Data
public class RiskWarningRecordPageResp {
    private Integer total;
    private Integer size;
    private Integer current;
    private Integer pages;
    private List<RiskWarningRecordResp> records;
}
