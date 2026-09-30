package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandleApproval;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 任务处置审批视图对象 dz_task_handle_approval
 *
 * @author kongweiguang
 * @date 2026-02-06
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DzTaskHandleApproval.class)
public class DzTaskHandleApprovalVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @ExcelProperty(value = "id")
    private Long id;

    /**
     * handle_id
     */
    @ExcelProperty(value = "handle_id")
    private Long handleId;

    /**
     * 会议类型: 1.处置管理  2.防御响应  3.预测模式
     */
    private Integer meetingType;

    /**
     * 用户id
     */
    @ExcelProperty(value = "用户id")
    private Long userId;

    /**
     * 用户名称
     */
    @ExcelProperty(value = "用户名称")
    private String nickname;

    /**
     * 审批类型（1：打勾 2：审批）
     */
    @ExcelProperty(value = "审批类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1=：打勾,2=：审批")
    private Integer type;

    /**
     * 处置任务步骤
     */
    @ExcelProperty(value = "处置任务步骤")
    private Integer process;

    /**
     * 审批状态（0：未审批 1：已审批）
     */
    @ExcelProperty(value = "审批状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=：未审批,1=：已审批")
    private Integer status;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createDate;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateDate;

    /**
     * 角色名称（关联 sys_user_role + sys_role 查询）
     */
    private String roleName;

    /**
     * 所属轮次
     */
    @ExcelProperty(value = "所属轮次")
    private Integer roundNo;

}
