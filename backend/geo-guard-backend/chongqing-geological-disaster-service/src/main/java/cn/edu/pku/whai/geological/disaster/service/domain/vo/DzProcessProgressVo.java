/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzProcessProgress;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DzProcessProgress.class)
public class DzProcessProgressVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键ID")
    private Long id;

    @ExcelProperty(value = "防御响应Id")
    private Long defId;

    @ExcelProperty(value = "流程状态")
    private Integer status;

    @ExcelProperty(value = "开启时间")
    private Date createDate;

    @ExcelProperty(value = "所属轮次")
    private Integer roundNo;

    @ExcelProperty(value = "动作类型")
    private Integer actionType;

    @ExcelProperty(value = "进度描述")
    private String description;
}
