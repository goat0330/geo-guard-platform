/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import cn.edu.pku.whai.geological.disaster.data.typehandler.PgJacksonTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 报灾管理对象 dz_report_disaster
 *
 * @author kongweiguang
 * @date 2026-01-27
 */
@Data
@TableName(value = "dz_report_disaster", autoResultMap = true)
public class DzReportDisaster implements Serializable {

    /**
     * 来源类型：任务反馈。
     */
    public static final Integer SOURCE_TYPE_TASK_FEEDBACK = 1;

    /**
     * 来源类型：群众报灾。
     */
    public static final Integer SOURCE_TYPE_PUBLIC_REPORT = 2;

    /**
     * 处置路线：巡查员核查。
     */
    public static final Integer PROCESS_TYPE_INSPECTOR_VERIFY = 1;

    /**
     * 处置路线：直接报送乡镇。
     */
    public static final Integer PROCESS_TYPE_DIRECT_TOWN = 2;

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户手机号
     */
    private String userPhone;

    /**
     * 用户的角色
     */
    private String userRole;

    /**
     * 打卡时间
     */
    private Date checkTime;

    /**
     * 打卡经纬度（如：POINT(109.5179247815959830.631124795779762)）
     */
    private String checkCenter;

    /**
     * 检查点的位置（中文）
     */
    private String checkCenterLocation;

    /**
     * 详细地址
     */
    private String detailedAddress;

    /**
     * 现场照片的ossId列表
     */
    private String photos;

    /**
     * 现场记录
     */
    private String sceneTextRecord;

    /**
     * 风险等级（与 dynamicRiskLevel 逻辑一致，数值越大风险越高：1：低 2：中 3：高 4：极高）
     */
    private Integer aiRiskLevel;

    /**
     * 风险标签
     */
    private String aiRiskLabel;

    /**
     * 报告详情
     */
    private String aiReportDetail;

    /**
     * AI 识图补充属性
     */
    @TableField(typeHandler = PgJacksonTypeHandler.class)
    private Map<String, Object> aiVisionProps;

    /**
     * 人工修正后的风险等级（与 dynamicRiskLevel 逻辑一致，数值越大风险越高：1：低 2：中 3：高 4：极高）
     */
    private Integer manualRiskLevel;

    /**
     * 人工修正备注
     */
    private String manualRiskRemark;

    /**
     * 状态（1：待处理 2：已报送 3：已处理）
     */
    private Integer status;

    /**
     * 来源类型（1：任务反馈 2：群众报灾）
     */
    private Integer sourceType;

    /**
     * 处置路线（1：巡查员核查 2：直接报送乡镇）
     */
    private Integer processType;

    /**
     * 关联任务派发表 dz_task_dist_list.id
     */
    private Long taskId;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;


}
