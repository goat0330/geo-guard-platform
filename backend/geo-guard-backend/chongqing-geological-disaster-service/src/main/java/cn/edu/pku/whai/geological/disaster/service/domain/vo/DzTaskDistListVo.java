/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import org.dromara.system.domain.vo.SysUserVo;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 任务派发清单视图对象 dz_task_dist_list
 *
 * @author kongweiguang
 * @date 2026-01-08
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DzTaskDistList.class)
public class DzTaskDistListVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @ExcelProperty(value = "id")
    private Long id;

    /**
     * 斜坡单元id
     */
    @ExcelProperty(value = "斜坡单元id")
    private String unitId;

    /**
     * 派发人员id
     */
    @ExcelProperty(value = "派发人员id")
    private Long userId;

    /**
     * 风险评估id
     */
    @ExcelProperty(value = "风险评估id")
    private Long riskId;

    /**
     * 巡查要求
     */
    @ExcelProperty(value = "巡查要求")
    private String submitRequire;

    /**
     * 巡查建议
     */
    @ExcelProperty(value = "巡查建议")
    private String inspectionSuggestion;


    /**
     * 现场照片（多条的话，分割）
     */
    @ExcelProperty(value = "现场照片")
    private String scenePhoto;

    /**
     * 文字记录
     */
    @ExcelProperty(value = "文字记录")
    private String textRecord;

    /**
     * 任务状态（1：未推送 2：未核查 3：核查中 4：已关闭 5：已反馈 6：申请技术协查 7：已过期）
     */
    @ExcelProperty(value = "任务状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1=未推送,2=未核查,3=核查中,4=已关闭,5=已反馈,6=申请技术协查,7=已过期")
    private Integer status;

    /**
     * APP推送类型：1人工推送 2系统推送
     */
    @ExcelProperty(value = "APP推送类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1=人工推送,2=系统推送")
    private Integer appPushType;

    /**
     * APP推送用户ID；系统推送固定为地象智能体(0)
     */
    @ExcelProperty(value = "APP推送用户ID")
    private Long appPushUserId;

    /**
     * APP推送时间
     */
    @ExcelProperty(value = "APP推送时间")
    private Date appPushTime;

    /**
     * 任务创建方式：1人工创建 2系统创建
     */
    @ExcelProperty(value = "任务创建方式", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1=人工创建,2=系统创建")
    private Integer taskCreateType;

    /**
     * 创建用户ID；系统创建固定为地象智能体(0)
     */
    @ExcelProperty(value = "任务创建用户ID")
    private Long taskCreateUserId;

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
     * 检查时间
     */
    @ExcelProperty(value = "检查时间")
    private Date checkTime;

    /**
     * 检查坐标中心点
     */
    @ExcelProperty(value = "检查坐标中心点")
    private String checkCenter;

    /**
     * 打卡坐标生成的地点信息
     */
    @ExcelProperty(value = "打卡地点信息")
    private String checkCenterLocation;

    /**
     * 提交时间
     */
    @ExcelProperty(value = "提交时间")
    private Date submitTime;

    /**
     * 来源（0：手动添加 1：系统评估 2：群众上报 3：防御响应 4：应急处置 5：监测预警 6：技术协查）
     */
    @ExcelProperty(value = "来源", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=手动添加,1=系统评估,2=群众上报,3=防御响应,4=应急处置,5=监测预警,6=技术协查")
    private Integer sourceType;

    /**
     * 群众上报时报告id
     */
    @ExcelProperty(value = "群众上报时报告id")
    private Long reportId;

    /**
     * 方案编号(关联方案表唯一标识)
     */
    @ExcelProperty(value = "方案编号(关联方案表唯一标识)")
    private Long handleId;

    /**
     * 防御响应方案id
     */
    @ExcelProperty(value = "防御响应方案id")
    private Long defId;

    /**
     * 上报信息JSON（仅险情核实任务使用）
     */
    @ExcelProperty(value = "上报信息JSON")
    private String reportInfo;

    /**
     * 任务来源业务描述
     */
    @ExcelProperty(value = "任务来源")
    private String taskSource;

    /**
     * 上游任务节点ID（技术协查任务填写原任务id）
     */
    @ExcelProperty(value = "上游任务节点ID")
    private Long relatedTaskId;

    /**
     * 任务类型（巡查任务 / AI险情核实 / 防御响应 / 现场处置 / 监测预警 / 应急调查 / 群众报灾）
     */
    @ExcelProperty(value = "任务类型")
    private String taskType;

    /**
     * 任务名称
     */
    @ExcelProperty(value = "任务名称")
    private String planName;

    /**
     * 任务类型: 1.人员安置/2.警示防护/3.监测巡查（群测群防）/4.排危除险/5.工程治理/6.宣传告知/7.交通管制/8.其他建议/9.监测巡查（仪器监测）
     */
    @ExcelProperty(value = "方案类型", converter = ExcelDictConvert.class)
    private Integer planType;

    /**
     * 责任人
     */
    @ExcelProperty(value = "责任人")
    private String responsiblePerson;

    /**
     * 责任人电话号码
     */
    @ExcelProperty(value = "责任人电话号码")
    private String responsiblePersonPhone;

    /**
     * 详细地址
     */
    @ExcelProperty(value = "详细地址")
    private String detailedAddress;

    /**
     * 短信正文快照
     */
    @ExcelProperty(value = "短信正文")
    private String smsContent;

    /**
     * 删除状态: 0. 未删除 1.已删除
     */
    @ExcelProperty(value = "删除状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=未删除,1=已删除")
    private Integer delete;

    /**
     * 系统评估时，系统评估人员信息
     */
    @ExcelProperty(value = "系统评估时，系统评估人员信息")
    private SysUserVo sysUser;

    private SlopeUnitVo slopeUnit;

    private String dailyNeedAttention;
    private String defenseNeedAttention;
    private Integer dynamicRiskLevel;

    /**
     * 是否逾期: 0. 未逾期 1.已逾期
     */
    @ExcelProperty(value = "是否逾期", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=：否,1=：是")
    private Integer overdue;

    /**
     * 配额已消耗: 0.未消耗 1.已消耗（仅风险区巡查员任务有效，用户有效完成时置1，与status解耦避免混用）
     */
    @ExcelProperty(value = "配额已消耗", converter = ExcelDictConvert.class)
    private Integer quotaConsumed;

    /**
     * 最后一次催办时间
     */
    @ExcelProperty(value = "最后一次催办时间")
    private Date lastRemindTime;

    /**
     * 催办次数
     */
    @ExcelProperty(value = "催办次数")
    private Integer reminderCount;

    /**
     * 任务关闭原因：如区域防御随乡镇/县级归档、范围缩小等由业务写入可读文案
     */
    @ExcelProperty(value = "任务关闭原因")
    private String closeReason;

    /**
     * 任务关闭时间：与 closeReason 同时写入，用于审计与列表展示
     */
    @ExcelProperty(value = "任务关闭时间")
    private Date closedTime;

    /**
     * 主表备注（dz_task_dist_list.remark）
     */
    @ExcelProperty(value = "主表备注")
    private String remark;

    /**
     * 任务备注（dz_task_dist_list_remark.remark）
     */
    @ExcelProperty(value = "任务备注")
    private String taskRemark;

    /**
     * 任务备注创建时间（dz_task_dist_list_remark.create_date）
     */
    @ExcelProperty(value = "任务备注创建时间")
    private Date taskRemarkCreateDate;

    /**
     * 任务备注人id（dz_task_dist_list_remark.user_id）
     */
    @ExcelProperty(value = "任务备注人id")
    private Long taskRemarkUserId;

    /**
     * 任务备注人姓名（sys_user.nick_name）
     */
    @ExcelProperty(value = "任务备注人姓名")
    private String taskRemarkUserName;

    /**
     * 任务上报备注（dz_task_dist_list_add.remark）
     */
    @ExcelProperty(value = "任务上报备注")
    private String addRemark;

    /**
     * 任务上报备注创建时间（dz_task_dist_list_add.create_date）
     */
    @ExcelProperty(value = "任务上报备注创建时间")
    private Date addRemarkCreateDate;

    /**
     * 任务上报人id（dz_task_dist_list_add.reporter_id）
     */
    @ExcelProperty(value = "任务上报人id")
    private Long addReportUserId;

    /**
     * 任务上报人姓名（sys_user.nick_name）
     */
    @ExcelProperty(value = "任务上报人姓名")
    private String addReportUserName;

    /**
     * 最新流程链路节点
     */
    private TaskProcessChainNodeVo latestProcessNode;
}
