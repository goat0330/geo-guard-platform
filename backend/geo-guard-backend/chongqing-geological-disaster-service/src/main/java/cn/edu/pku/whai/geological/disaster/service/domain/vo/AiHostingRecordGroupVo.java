/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * AI 托管历史记录时间窗口展示对象。
 */
@Data
public class AiHostingRecordGroupVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 时间窗口起始时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date timestamp;

    /**
     * 该时间窗口内的 AI 托管历史记录。
     */
    private List<AiHostingRecordVo> records;
}
