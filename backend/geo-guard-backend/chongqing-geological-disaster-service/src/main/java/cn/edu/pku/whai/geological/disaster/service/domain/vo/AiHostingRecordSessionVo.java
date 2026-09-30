/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzAutoModeAiHostingRecord;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * AI托管运行记录。
 */
@Data
@AutoMapper(target = DzAutoModeAiHostingRecord.class)
public class AiHostingRecordSessionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String recordName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date openedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date closedAt;

    private Integer durationMinutes;

    private String durationText;

    private Boolean running;

    /**
     * 该托管记录对应的概览数据。
     */
    private AiHostingOverviewVo overview;

    /**
     * 该托管记录窗口内按时间戳分组的历史展示记录。
     */
    private List<AiHostingRecordGroupVo> historyRecords;

    private Long openUserId;

    private String openUserName;

    private String openUserRole;

    private Long closeUserId;

    private String closeUserName;

    private String closeUserRole;
}
