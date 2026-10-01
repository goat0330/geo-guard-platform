/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 风险预警记录表
 */
@Data
@TableName("dz_risk_warning_record")
public class RiskWarningRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键id */
    @TableId(value = "id")
    private String id;

    /** 预警日期 */
    private Date warningDate;

    /** 预警模型id */
    private Integer warningModelId;

    /** 预警模型 */
    private String warningModel;

    /** 状态文本 */
    private String stateText;

    /** 步骤信息 */
    private String stepInfo;

    /** 预警时效(1小时 3小时 6小时 12小时 24小时 36小时) */
    private Short warningTimeLimit;

    /** 结果显示模式(显示填充区 只显边区) */
    private String resultDisplayMode;

    /** 分析时间 */
    private Date analysisTime;

    /** 分析人id */
    private String analystId;

    /** 分析人 */
    private String analyst;

    /** 分析类型(日常预警(预报预警) 短临预警) */
    private String analysisType;

    /** 状态(0->分析中 1->已发布 2->发起签批 3->签批通过 4->签批未通过 5->分析完成) */
    private String status;

    /** 发布人id */
    private String publisherId;

    /** 发布人 */
    private String publisher;

    /** 结果表名称 */
    private String resultTableName;

    /** 预报词 */
    private String forecastWords;

    /** 删除状态(0->未删除 1->已删除) */
    private String deleteStatus;

    /** 发布对象 */
    private String publishObject;

    /** 最大预警等级 */
    private Integer maxWarningLevel;

    /** 最大预警等级text */
    private String maxWarningLevelText;

    /** 是否发起签批(0->未发起 1->已发起)(废弃) */
    private String isInitiateApproval;

    /** 发布时间 */
    private Date publishTime;

    /** 是否做过面积统计(0->没有 1->有) */
    private String isAreaStatistics;

    /** 错误信息 */
    private String errorMessage;

    /** 是否分析完成(0->未完成 1->已完成 -1->分析失败) */
    private String isAnalysisCompleted;

    /** 是否最新发布(0->否 1->是) */
    private String isLatestPublish;

    /** 是否发布服务(0->否 1->是 -1->无法发布服务 -2->调取发布服务的接口失败) */
    private String isPublishService;

    /** 发起会签人id */
    private String initiatorId;

    /** 发起会签人姓名 */
    private String initiatorName;

    /** 会商词 */
    private String consultationWords;

    /** 发起会签时间 */
    private Date initiateTime;

    /** 预警起点时间(例如 早八/晚八) */
    private Date warningStartTime;

    /** 是否加密 */
    private String isEncrypted;

    /** 会商相关字段 */
    private String consultation;

    /** 更新时间 */
    private Date updateTime;

    /** 短信模板 */
    private String smsTemplate;

    /** 执行进度 */
    private Short executionProgress;

    /** 执行步骤信息 */
    private String executionStepInfo;

    /** 短信调整内容 */
    private String smsAdjustContent;

    /** 短信调整时间 */
    private Date smsAdjustTime;

    /** 短信调整人 */
    private String smsAdjustPerson;

    /** 会商发起人id */
    private String consultationInitiatorId;

    /** 会商发起人姓名 */
    private String consultationInitiatorName;

    /** 会商发起时间 */
    private Date consultationInitiateTime;

    /** 行政区划 */
    private String administrativeDivision;

    /** 等级 */
    private String level;
}
