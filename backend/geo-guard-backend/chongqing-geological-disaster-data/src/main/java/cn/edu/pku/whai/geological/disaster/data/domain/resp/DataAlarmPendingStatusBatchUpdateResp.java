/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class DataAlarmPendingStatusBatchUpdateResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实际更新成功的预警id
     */
    private List<Long> updatedIds;

    /**
     * 被过滤的预警项
     */
    private List<FilteredItem> filteredItems;

    @Data
    public static class FilteredItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;

        private String reason;
    }
}
