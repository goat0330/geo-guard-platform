/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
public class StatThirdWarningStatusVo {
    private Integer total;
    private List<StatThirdWarningStatusItem> items;

    @Data
    @RequiredArgsConstructor
    public static class StatThirdWarningStatusItem {
        private final String warningType;
        private final Long warningCount;
    }

}
