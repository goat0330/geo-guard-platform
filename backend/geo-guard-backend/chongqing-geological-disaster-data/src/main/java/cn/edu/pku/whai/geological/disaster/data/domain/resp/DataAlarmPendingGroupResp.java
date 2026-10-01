/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class DataAlarmPendingGroupResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date time;

    private List<Item> source;

    @Data
    public static class Item implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;

        private String city;

        @JsonProperty("level_streets_json")
        private String levelStreetsJson;

        @JsonProperty("create_date")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createDate;

        @JsonProperty("publish_date")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date publishDate;

        @JsonProperty("valid_start_date")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date validStartDate;

        @JsonProperty("valid_end_date")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date validEndDate;

        @JsonProperty("key_tips")
        private String keyTips;

        @JsonProperty("pending_status")
        private Integer pendingStatus;
    }
}
