/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.resp.warning;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class RiskWarningRecordResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @Alias("id")
    private String id;

    /**
     * 预警日期
     */
    @Alias("yjrq")
    private Date warningDate;

    /**
     * 预警模型id
     */
    @Alias("yjmxid")
    private Integer warningModelId;

    /**
     * 预警模型
     */
    @Alias("yjmx")
    private String warningModel;

    /**
     *
     */
    @Alias("statetext")
    private String stateText;


    /**
     *
     */
    @Alias("stepinfo")
    private String stepInfo;

    /**
     * 预警时效(1小时 3小时 6小时  12小时 24小时 36小时 )
     */
    @Alias("yjsx")
    private Short warningTimeLimit;

    /**
     * 结果显示模式(显示填充区  只显边区)
     */
    @Alias("jgxsms")
    private String resultDisplayMode;

    /**
     * 分析时间
     */
    @Alias("fxsj")
    private Date analysisTime;

    /**
     * 分析人id
     */
    @Alias("fxrid")
    private String analystId;

    /**
     * 分析人
     */
    @Alias("fxr")
    private String analyst;

    /**
     * 分析类型(日常预警(预报预警) 短临预警)
     */
    @Alias("fxlx")
    private String analysisType;

    /**
     * 状态(0->分析中 1->已发布 2->发起签批 3->签批通过 4->签批未通过 5->分析完成)
     */
    @Alias("state")
    private String status;

    /**
     * 发布人id
     */
    @Alias("fbrid")
    private String publisherId;

    /**
     * 发布人
     */
    @Alias("fbr")
    private String publisher;

    /**
     * 结果表名称
     */
    @Alias("jgtbname")
    private String resultTableName;

    /**
     * 预报词
     */
    @Alias("ybc")
    private String forecastWords;

    /**
     * 删除状态(0->未删除 1->已删除)
     */
    @Alias("deltate")
    private String deleteStatus;

    /**
     * 发布对象
     */
    @Alias("fbdx")
    private String publishObject;

    /**
     * 最大预警等级
     */
    @Alias("maxyjdj")
    private Integer maxWarningLevel;

    /**
     * 最大预警等级text
     */
    @Alias("maxyjdjtext")
    private String maxWarningLevelText;

    /**
     * 是否发起签批(0->未发起 1->已发起)(废弃)
     */
    @Alias("sffqqp")
    private String isInitiateApproval;

    /**
     * 发布时间
     */
    @Alias("fbsj")
    private Date publishTime;

    /**
     * 是否做过面积统计(0->没有 1->有)
     */
    @Alias("istjmj")
    private String isAreaStatistics;

    /**
     * 错误信息
     */
    @Alias("erromsg")
    private String errorMessage;

    /**
     * 是否分析完成(0->未完成 1->已完成   -1->分析失败)
     */
    @Alias("isfxwc")
    private String isAnalysisCompleted;

    /**
     * 是否最新发布(0->否 1->是)
     */
    @Alias("isnewfb")
    private String isLatestPublish;

    /**
     * 是否发布服务(0->否 1->是 -1->无法发布服务,数据表不存在 -2->调取发布服务的接口失败)
     */
    @Alias("isfbfw")
    private String isPublishService;

    /**
     * 发起会签人id
     */
    @Alias("fqrid")
    private String initiatorId;

    /**
     * 发起会签人姓名
     */
    @Alias("fqrname")
    private String initiatorName;

    /**
     * 会商词
     */
    @Alias("hsc")
    private String consultationWords;

    /**
     * 发起会签时间
     */
    @Alias("fqsj")
    private Date initiateTime;

    /**
     * 预警起点时间(例如 早八/晚八)
     */
    @Alias("yjqdsj")
    private Date warningStartTime;

    /**
     * 是否加密
     */
    @Alias("flag")
    private String isEncrypted;

    /**
     * 会商相关字段
     */
    @Alias("huishang")
    private String consultation;

    /**
     * 更新时间
     */
    @Alias("updatetime")
    private Date updateTime;

    /**
     * 短信模板
     */
    @Alias("msg")
    private String smsTemplate;

    /**
     * 执行进度
     */
    @Alias("step")
    private Short executionProgress;

    /**
     * 执行步骤信息
     */
    @Alias("stepinfo")
    private String executionStepInfo;

    /**
     * 短信调整内容
     */
    @Alias("dxtznr")
    private String smsAdjustContent;

    /**
     * 短信调整时间
     */
    @Alias("dxtzsj")
    private Date smsAdjustTime;

    /**
     * 短信调整人
     */
    @Alias("dxtzr")
    private String smsAdjustPerson;

    /**
     * 会商发起人id
     */
    @Alias("hsfqrid")
    private String consultationInitiatorId;

    /**
     * 会商发起人姓名
     */
    @Alias("hsfqrname")
    private String consultationInitiatorName;

    /**
     * 会商发起时间
     */
    @Alias("hsfqsj")
    private Date consultationInitiateTime;

    /**
     * 行政区划
     */
    @Alias("xzqh")
    private String administrativeDivision;

    /**
     * 等级
     */
    @Alias("level")
    private String level;
}
