/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandleDetailContent;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 处置管理详情内容视图对象 dz_task_handle_detail_content
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DzTaskHandleDetailContent.class)
public class DzTaskHandleDetailContentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @ExcelProperty(value = "主键id")
    private Long id;

    /**
     * 业务主键
     */
    @ExcelProperty(value = "业务主键")
    private Long bizId;

    /**
     * 业务类型：1-dz_task_handle，2-dz_def_resp_plan
     */
    @ExcelProperty(value = "业务类型")
    private Integer bizType;

    /**
     * 内容类型：1-应急调查报告，2-撤离方案，3-防御响应方案，4-初始报告，5-最终报告，6-复盘报告，7-会商确认
     */
    @ExcelProperty(value = "内容类型")
    private Integer contentType;

    /**
     * 内容文本
     */
    @ExcelProperty(value = "内容文本")
    private String planContent;

    /**
     * 内容结构化内容
     */
    @ExcelProperty(value = "内容结构化内容")
    private String planContentJson;

    /**
     * 专家建议/提示词
     */
    @ExcelProperty(value = "专家建议")
    private String suggest;

    /**
     * 主持人Id
     */
    @ExcelProperty(value = "主持人Id")
    private Long userId;

    /**
     * 主持人姓名
     */
    @ExcelProperty(value = "主持人姓名")
    private String userName;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createDate;

    /**
     * 删除状态：0-未删除，1-已删除
     */
    @ExcelProperty(value = "删除状态")
    private Integer deleted;

    /**
     * 是否最新：0-否，1-是
     */
    @ExcelProperty(value = "是否最新")
    private Integer isLatest;

    /**
     * 所属轮次
     */
    @ExcelProperty(value = "所属轮次")
    private Integer roundNo;

    /**
     * 状态：0-草稿，1-已提交，2-通过
     */
    @ExcelProperty(value = "状态")
    private Integer status;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateDate;
}
