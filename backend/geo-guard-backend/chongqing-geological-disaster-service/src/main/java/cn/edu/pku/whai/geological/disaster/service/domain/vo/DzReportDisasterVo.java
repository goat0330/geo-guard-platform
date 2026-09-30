/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;


/**
 * 报灾管理视图对象 dz_report_disaster
 *
 * @author kongweiguang
 * @date 2026-01-27
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DzReportDisaster.class)
public class DzReportDisasterVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @ExcelProperty(value = "id")
    private Long id;

    /**
     * 用户id
     */
    @ExcelProperty(value = "用户id")
    private Long userId;

    /**
     * 用户名
     */
    @ExcelProperty(value = "用户名")
    private String userName;

    /**
     * 用户手机号
     */
    @ExcelProperty(value = "用户手机号")
    private String userPhone;

    /**
     * 用户的角色
     */
    @ExcelProperty(value = "用户的角色")
    private String userRole;

    /**
     * 打卡时间
     */
    @ExcelProperty(value = "打卡时间")
    private Date checkTime;

    /**
     * 打卡经纬度（如：POINT(109.5179247815959830.631124795779762)）
     */
    @ExcelProperty(value = "打卡经纬度", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "如=：POINT(109.5179247815959830.631124795779762)")
    private String checkCenter;

    /**
     * 检查点的位置（中文）
     */
    @ExcelProperty(value = "检查点的位置", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "中=文")
    private String checkCenterLocation;

    /**
     * 详细地址
     */
    @ExcelProperty(value = "详细地址")
    private String detailedAddress;

    /**
     * 现场照片的ossId列表
     */
    @ExcelProperty(value = "现场照片的ossId列表")
    private String photos;

    /**
     * 现场记录
     */
    @ExcelProperty(value = "现场记录")
    private String sceneTextRecord;

    /**
     * 风险等级（与 dynamicRiskLevel 逻辑一致，数值越大风险越高；导出时转换为中文）
     */
    @ExcelProperty(value = "风险等级", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1=：低,2=：中,3=：高,4=：极高")
    private Integer aiRiskLevel;

    /**
     * 风险标签
     */
    @ExcelProperty(value = "风险标签")
    private String aiRiskLabel;

    /**
     * 报告详情
     */
    @ExcelProperty(value = "报告详情")
    private String aiReportDetail;

    /**
     * AI 识图补充属性
     */
    private Map<String, Object> aiVisionProps;

    /**
     * 人工修正后的风险等级（与 dynamicRiskLevel 逻辑一致，数值越大风险越高；导出时转换为中文）
     */
    @ExcelProperty(value = "人工修正风险等级", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1=：低,2=：中,3=：高,4=：极高")
    private Integer manualRiskLevel;

    /**
     * 人工修正备注
     */
    @ExcelProperty(value = "人工修正备注")
    private String manualRiskRemark;

    /**
     * 状态（1：待处理 2：已报送 3：已处理）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1=：待处理,2=：已报送,3=：已关闭,4=：已核查")
    private Integer status;

    /**
     * 来源类型（1：任务反馈 2：群众报灾）
     */
    @ExcelProperty(value = "来源类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1=：任务反馈,2=：群众报灾")
    private Integer sourceType;

    /**
     * 处置类型
     */
    @ExcelProperty(value = "处置类型")
    @ExcelDictFormat(readConverterExp = "1=：巡查员核查,2=：直接报送乡镇")
    private Integer processType;

    /**
     * 关联任务派发表 dz_task_dist_list.id
     */
    @ExcelProperty(value = "关联任务id")
    private Long taskId;

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
     * 任务派发对象名称
     */
    private String dispatchTargetName;

    /**
     * 所属区/县/县级市全称
     */
    private String county;

    /**
     * 所属街道全称
     */
    private String street;

    /**
     * 所属社区全称
     */
    private String village;

}
