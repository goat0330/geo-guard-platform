/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.DataAlarm;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DataAlarm.class)
public class DataAlarmVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键ID")
    private Long id;

    @ExcelProperty(value = "编号")
    private String code;

    @ExcelProperty(value = "会话ID")
    private String sessionId;

    private Integer level;

    @ExcelProperty(value = "所属市")
    private String city;

    private String streets;

    @ExcelProperty(value = "预警等级与乡镇映射JSON")
    private String levelStreetsJson;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ExcelProperty(value = "预警发布日期")
    private Date time;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty(value = "报告发布时间")
    private Date publishDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty(value = "预警发布有效开始时间")
    private Date validStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty(value = "预警发布有效结束时间")
    private Date validEndDate;

    @ExcelProperty(value = "状态（0:未触发 1:已触发 2:已过期）")
    private Integer status;

    @ExcelProperty(value = "待触发状态（0:无 1:待触发 2:已触发）")
    private Integer pendingStatus;

    @ExcelProperty(value = "创建时间")
    private Date createDate;

    private Integer latestCreateDateFlag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty(value = "更新时间")
    private Date updateDate;

    @ExcelProperty(value = "消息来源")
    private String source;

    @ExcelProperty(value = "警报来源类型")
    private Integer sourceType;

    @ExcelProperty(value = "原始消息")
    private String message;

    @ExcelProperty(value = "关键提示")
    private String keyTips;
}
