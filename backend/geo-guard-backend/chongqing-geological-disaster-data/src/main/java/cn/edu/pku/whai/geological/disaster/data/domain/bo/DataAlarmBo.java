/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.data.domain.po.DataAlarm;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DataAlarm.class, reverseConvertGenerate = false)
public class DataAlarmBo extends BaseEntity {

    private Long id;

    private String code;

    private String sessionId;

    private Integer level;

    private String city;

    private String streets;

    private String levelStreetsJson;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date time;

    /**
     * 报告发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date publishDate;

    /**
     * 预警发布有效开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validStartDate;

    /**
     * 预警发布有效结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validEndDate;

    /**
     * 是否仅查询未过期数据（valid_end_date &gt;= 当前时间）；false 表示仅查询已过期数据
     */
    private Boolean onlyNotExpired;

    /**
     * 状态（0:未触发 1:已触发 2:已过期）
     */
    private Integer status;

    /**
     * 待触发状态（0:无 1:待触发 2:已触发）
     */
    private Integer pendingStatus;

    private Date createDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateDate;

    private String source;

    /**
     * 警报来源类型（1:人工补录 2:解析报告）
     */
    private Integer sourceType;

    private String message;

    private String keyTips;
}
