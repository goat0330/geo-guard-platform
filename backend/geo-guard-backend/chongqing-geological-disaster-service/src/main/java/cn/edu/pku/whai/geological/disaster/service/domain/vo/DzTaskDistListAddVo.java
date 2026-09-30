package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistListAdd;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 任务上报信息视图对象 dz_task_dist_list_add
 *
 * @author kongweiguang
 * @date 2026-04-15
 */
@Data
@AutoMapper(target = DzTaskDistListAdd.class)
public class DzTaskDistListAddVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务 id
     */
    @ExcelProperty(value = "任务 id")
    private Long taskId;

    /**
     * 用户 id
     */
    @ExcelProperty(value = "接收人员id")
    private Long userId;

    /**
     * 上报内容
     */
    @ExcelProperty(value = "上报内容")
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createDate;

    /**
     * 上报人id
     */
    @ExcelProperty(value = "上报人id")
    private Long reportUserId;

    /**
     * 上报人名称
     */
    @ExcelProperty(value = "上报人名称")
    private String reportUserName;
}
