/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
public class StatThirdWarningTypeVo {
    private Integer total;
    private List<StatThirdWarningTypeItem> items;

    @Data
    @RequiredArgsConstructor
    public static class StatThirdWarningTypeItem {
        private final String warningLevel;
        private final Long warningCount;
    }

}
