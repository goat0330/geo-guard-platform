/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class DzDefProcessProgressVo {

    @ExcelProperty(value = "流程描述")
    private String description;

    @ExcelProperty(value = "流程阶段")
    private Integer status;

    @ExcelProperty(value = "开启时间")
    private Date createDate;

    @ExcelProperty(value = "所属轮次")
    private Integer roundNo;

    @ExcelProperty(value = "动作类型")
    private Integer actionType;
}
