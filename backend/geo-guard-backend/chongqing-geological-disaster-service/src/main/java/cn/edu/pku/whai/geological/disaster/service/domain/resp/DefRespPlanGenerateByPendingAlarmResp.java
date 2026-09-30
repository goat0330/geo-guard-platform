/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.resp;

import lombok.Data;

import java.util.List;

@Data
public class DefRespPlanGenerateByPendingAlarmResp {

    private List<Long> triggeredIds;

    private List<FilteredItem> filteredItems;

    @Data
    public static class FilteredItem {

        private Long id;

        private String reason;
    }
}
