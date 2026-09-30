package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskDistListBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistChainScopeActionBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistChainScopePushBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistPushBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistStatStatusBo;
import cn.edu.pku.whai.geological.disaster.service.domain.req.McpTaskDistListReq;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.*;
import cn.edu.pku.whai.geological.disaster.service.sms.defresp.DefRespSmsEventSnapshot;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 任务派发清单 Service 接口
 *
 * @author kongweiguang
 * @date 2026-01-08
 */
public interface IDzTaskDistListService {

    /**
     * 查询任务派发清单
     *
     * @param id 主键
     * @return 任务派发清单
     */
    DzTaskDistListVo queryById(Long id);

    /**
     * 按历史记录主键查询任务提交历史。
     *
     * @param id 历史记录主键
     * @return 任务提交历史
     */
    DzTaskDistListHistoryVo queryHistoryById(Long id);

    /**
     * 通过任务 id 查询整条流程链路。
     *
     * @param taskId 任务 id
     * @return 流程链路节点列表
     */
    List<TaskProcessChainNodeVo> queryProcessChain(Long taskId);

    /**
     * 通过链路 id 查询整条流程链路。
     *
     * @param chainId 链路 id
     * @return 流程链路节点列表
     */
    List<TaskProcessChainNodeVo> queryProcessChain(String chainId);

    /**
     * 通过任务 id 查询整条流程链路涉及的能力字段。
     *
     * @param taskId 任务 id
     * @return 能力字段列表
     */
    TaskProcessCapabilityVo queryProcessChainCapabilities(Long taskId);

    /**
     * 通过链路 id 查询整条流程链路涉及的能力字段。
     *
     * @param chainId 链路 id
     * @return 能力字段列表
     */
    TaskProcessCapabilityVo queryProcessChainCapabilities(String chainId);

    /**
     * 通过链路 id 或任务 id 查询整条流程链路相关任务的提交历史。
     *
     * @param chainId 链路 id，优先使用
     * @param taskId  任务 id
     * @return 提交历史列表
     */
    List<DzTaskDistListHistoryVo> queryProcessChainHistories(String chainId, Long taskId);

    /**
     * 通过任务 id 查询 AI 分析与推理链。
     *
     * @param taskId 任务 id
     * @return 普通链路与 AI 分析结果
     */
    TaskAiProcessChainVo queryAiProcessChain(Long taskId);

    /**
     * 通过链路 id 查询 AI 分析与推理链。
     *
     * @param chainId 链路 id
     * @return 普通链路与 AI 分析结果
     */
    TaskAiProcessChainVo queryAiProcessChain(String chainId);

    /**
     * 分页查询任务派发清单列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 任务派发清单分页列表
     */
    TableDataInfo<DzTaskDistListVo> queryPageList(DzTaskDistListBo bo, PageQuery pageQuery);

    /**
     * 分页查询任务派发清单列表，并补充每个任务的最新流程链路节点
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 任务派发清单分页列表
     */
    TableDataInfo<DzTaskDistListVo> queryPageListWithLatestProcessNode(DzTaskDistListBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的任务派发清单列表
     *
     * @param bo 查询条件
     * @return 任务派发清单列表
     */
    List<DzTaskDistListVo> queryList(DzTaskDistListBo bo);

    /**
     * 新增任务派发清单
     *
     * @param bo 任务派发清单
     * @return 是否新增成功
     */
    Boolean insertByBo(DzTaskDistListBo bo);

    /**
     * 批量新增日常巡查任务
     */
    Map<String, Integer> batchInsertPatrol();

    /**
     * 批量新增防御响应任务
     *
     * @return 创建的任务数量
     */
    Integer batchInsertDefRespPatrol();

    /**
     * 根据风险评估记录批量创建日常巡逻任务，创建规则与定时任务保持一致。
     *
     * @param riskIds 风险评估 id 列表
     * @return 创建成功的任务数量
     */
    Integer createDailyPatrolTasksByRiskIds(List<Long> riskIds);

    /**
     * 根据风险评估记录刷新监测预警任务，并按配置决定是否推送 APP。
     *
     * @param riskId 风险评估 id
     * @param autoPushTask 是否自动推送新任务到 APP；已下发任务更新仍同步 APP
     * @param sendTaskSms 是否在自动推送成功后发送普通任务短信
     * @return 新增或更新的任务数量
     */
    Integer upsertMonitorWarningTaskByRiskId(Long riskId, boolean autoPushTask, boolean sendTaskSms);

    /**
     * 修改任务派发清单
     *
     * @param bo 任务派发清单
     * @return 是否修改成功
     */
    Boolean updateByBo(DzTaskDistListBo bo);

    /**
     * 校验并批量删除任务派发清单信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    List<TaskDistStatStatusVo> statStatus(TaskDistStatStatusBo bo);

    List<TaskDistStatSourceTypeVo> statSourceType(TaskDistStatStatusBo bo);

    TaskDistPushVo push(TaskDistPushBo bo);

    List<TaskDistSmsPreviewItemVo> previewSms(TaskDistPushBo bo);

    Object pushCompatible(TaskDistChainScopePushBo bo);

    List<TaskDistSmsPreviewItemVo> previewSmsCompatible(TaskDistChainScopePushBo bo);

    TaskDistChainPushResultVo pushByChainScope(TaskDistChainScopePushBo bo);

    TaskDistChainPreviewResultVo previewSmsByChainScope(TaskDistChainScopePushBo bo);

    /**
     * 预览启动短信
     *
     * @param defId 防御响应方案id
     * @return 启动短信预览列表
     */
    List<DefRespStartSmsPreviewVo> previewDefRespStartSms(Long defId);

    /**
     * 预览区域防御响应启动、结束或调整短信。
     */
    List<DefRespStartSmsPreviewVo> previewDefRespLifecycleSms(DefRespSmsEventSnapshot event);

    /**
     * 发送区域防御响应启动、结束或调整短信。
     *
     * @param event 生命周期事件快照
     * @param manual 是否人工补发；人工补发不受自动发送幂等限制
     */
    SmsSendSummaryVo sendDefRespLifecycleSms(DefRespSmsEventSnapshot event, boolean manual);

    void remind(Long id);

    TaskDistChainScopeActionResultVo remindByChainScope(TaskDistChainScopeActionBo bo);

    TaskDistChainScopeActionResultVo deleteByChainScope(TaskDistChainScopeActionBo bo);

    List<DzTaskDistListVo> selectTaskDistList(McpTaskDistListReq req);

    /**
     * 根据方案 Id 批量生成处置任务
     *
     * @param handleId 处理任务 id
     * @return 是否生成成功
     */
    Boolean batchGenerateByHandleId(Long handleId);

    /**
     * 根据方案 Id 按指定 planType 批量生成处置任务
     *
     * @param handleId 处理任务 id
     * @param planType 指定计划类型；为空时生成全部
     * @return 是否生成成功
     */
    Boolean batchGenerateByHandleId(Long handleId, Integer planType);

    /**
     * 更新 APP 应急调查任务状态为已反馈，并同步 APP 更新接口。
     *
     * @param taskId 任务 id
     * @param handleId 当前处置 id
     * @param closedTime 关闭时间
     * @param closeReason 关闭原因
     * @return 关闭后的任务
     */
    DzTaskDistList closeLinkedEmergencyInvestigationTask(Long taskId, Long handleId, Date closedTime, String closeReason);

    /**
     * 根据范围更新日常巡逻任务的建议信息，仅对仍在执行的任务进行字段更新，不新增任务
     *
     * @param streets 街道范围
     * @param regId   区域防御响应方案 id
     * @return 更新成功的任务数量
     */
    Integer batchGenerateByRange(List<String> streets, Long regId);

    /**
     * 根据斜坡单元更新日常巡逻任务，仅对仍在执行的任务进行字段更新，不新增任务
     *
     * @param unitIds 斜坡单元 id 列表
     * @param defId   防御响应方案 id
     * @return 更新成功的任务数量
     */
    Integer batchGenerateByUnitIds(List<String> unitIds, Long defId);

    /**
     * 根据防御响应方案批量创建告警任务（监测员、风险区巡查员）
     *
     * @param defId 防御响应方案 id
     * @return 创建的任务数量
     */
    Integer batchCreateAlertTasksByDefId(Long defId);

    /**
     * 根据防御响应方案和指定斜坡单元批量创建告警任务（监测员、风险区巡查员）
     *
     * @param defId   防御响应方案 id
     * @param unitIds 指定斜坡单元 id 列表
     * @return 创建的任务数量
     */
    Integer batchCreateAlertTasksByUnitIds(Long defId, List<String> unitIds);

    /**
     * 定时补齐监测员任务：对处于告警状态的防御响应方案按斜坡单元每日最多生成一次监测员任务
     *
     * @return 本次创建的任务数量
     */
    Integer scheduleCreateMonitorTasks();

    /**
     * 每日8点30分生成防御响应巡查员任务，每个斜坡单元每日仅生成一次
     *
     * @return 创建的任务数量
     */
    void scheduleCreatePatrolQuotaTasks();

    /**
     * 风险区巡查员在兜底提醒时间窗内按配置间隔催办
     *
     * @return 催办的任务数量
     */
    Integer schedulePatrolTaskReminders();

    /**
     * 自动推送未推送任务。
     *
     * @param sourceTypes 允许自动推送的任务来源
     * @param limit 单轮最多处理条数
     * @param sendTaskSms 是否发送普通任务短信
     * @return 成功推送的任务数量
     */
    Integer autoPushUnpushedTasks(List<Integer> sourceTypes, int limit, boolean sendTaskSms);

    /**
     * 自动推送未推送日常巡逻任务。
     *
     * @param limit 单轮最多处理条数
     * @param sendTaskSms 是否发送普通任务短信
     * @return 成功推送的任务数量
     */
    Integer autoPushDailyPatrolTasks(int limit, boolean sendTaskSms);

    /**
     * 自动催办未完成任务。
     *
     * @param sourceTypes 允许自动催办的任务来源
     * @param limit 单轮最多处理条数
     * @return 成功催办的任务数量
     */
    Integer autoRemindOpenTasks(List<Integer> sourceTypes, int limit);

    /**
     * 自动推送指定报灾记录生成的任务。
     *
     * @param reportId 报灾记录 ID
     * @param sendTaskSms 是否发送普通任务短信
     * @return 成功推送的任务数量
     */
    Integer autoPushTasksByReportId(Long reportId, boolean sendTaskSms);

    /**
     * 关闭今天以前未完成的风险区巡查任务（source_type=1/3），并标记逾期。
     *
     * @return 关闭的任务数量
     */
    Integer closePreviousDayPatrolTasks();


    /**
     * 获取巡查规则统计信息
     *
     * @param date 指定日期，为空时默认今日
     * @return 巡查规则统计结果
     */
    InspectionRuleStatsVo getInspectionRuleStats(LocalDate date);

    /**
     * 统计每日任务数量
     *
     * @param startDateStr 开始日期（格式：YYYY-MM-DD）
     * @param endDateStr   结束日期（格式：YYYY-MM-DD）
     * @return 每日统计列表
     */
    List<TaskDistDayStatVo> statDay(String startDateStr, String endDateStr);

    /**
     * 关闭指定防御响应方案下仍处于开放态的防御来源任务，并写入关闭原因/时间。
     *
     * @param defId  防御响应方案 id（乡镇行）
     * @param reason 关闭原因
     * @return 关闭的任务条数
     */
    @SuppressWarnings("UnusedReturnValue")
    int closeOpenDefRespTasksByDefId(Long defId, String reason);

    /**
     * 按乡镇级防御响应方案 id 批量刷新防御响应任务的巡查建议，并同步 APP 任务内容。
     *
     * @param defIds 乡镇级防御响应方案 id 列表
     * @return 实际更新的任务数量
     */
    int refreshDefenseTaskSuggestionByDefIds(Collection<Long> defIds);

    /**
     * 按防御响应方案ID手动触发聚合短信发送。
     *
     * @param defId 防御响应方案 id
     * @return 发送、失败和配置跳过数量汇总
     */
    SmsSendSummaryVo sendDefRespSmsByDefId(Long defId);
}
