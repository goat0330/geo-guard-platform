/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzDefRespAlarmHistory;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DzDefRespAlarmHistory.class)
public class DzDefRespAlarmHistoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键id")
    private Long id;

    @ExcelProperty(value = "警报主键id")
    private Long alarmId;

    @ExcelProperty(value = "触发的防御响应主键id")
    private Long defId;

    @ExcelProperty(value = "警报编号")
    private String alarmCode;

    @ExcelProperty(value = "触发时间")
    private Date triggerTime;

    @ExcelProperty(value = "触发的防御响应等级")
    private Integer defRespLevel;

    @ExcelProperty(value = "警报等级")
    private Integer alarmLevel;

    @ExcelProperty(value = "警报来源")
    private String alarmSource;

    @ExcelProperty(value = "警报来源类型")
    private Integer alarmSourceType;

    @ExcelProperty(value = "创建时间")
    private Date createDate;
}
