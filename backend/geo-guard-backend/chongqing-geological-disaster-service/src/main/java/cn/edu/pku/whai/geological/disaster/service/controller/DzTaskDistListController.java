package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskDistListBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistChainScopeActionBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistChainScopePushBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistPushBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistStatStatusBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.*;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;


/**
 * 任务派发清单
 *
 * @author kongweiguang
 * @date 2026-01-08
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/taskDistList")
public class DzTaskDistListController extends BaseController {
    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[]{
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyyMMdd")
    };

    private final IDzTaskDistListService dzTaskDistListService;

    /**
     * 查询任务派发清单列表
     */
    @GetMapping("/list")
    public TableDataInfo<DzTaskDistListVo> list(DzTaskDistListBo bo, PageQuery pageQuery) {
        return dzTaskDistListService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询任务派发清单列表，并返回每个任务的最新流程链路节点
     */
    @GetMapping("/list/latest-process-node")
    public TableDataInfo<DzTaskDistListVo> listWithLatestProcessNode(DzTaskDistListBo bo, PageQuery pageQuery) {
        return dzTaskDistListService.queryPageListWithLatestProcessNode(bo, pageQuery);
    }

    /**
     * 查询所有任务(不分页)
     */
    @GetMapping("/listAll")
    public R<List<DzTaskDistListVo>> listAll(DzTaskDistListBo bo) {
        return R.ok(dzTaskDistListService.queryList(bo));
    }

    /**
     * 导出任务派发清单列表
     */
    @Log(title = "任务派发清单", businessType = BusinessType.EXPORT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/export")
    public void export(DzTaskDistListBo bo, HttpServletResponse response) {
        List<DzTaskDistListVo> list = dzTaskDistListService.queryList(bo);
        ExcelUtil.exportExcel(list, "任务派发清单", DzTaskDistListVo.class, response);
    }

    /**
     * 获取任务派发清单详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<DzTaskDistListVo> getInfo(@NotNull(message = "主键不能为空")
                                       @PathVariable Long id) {
        return R.ok(dzTaskDistListService.queryById(id));
    }

    /**
     * 通过历史记录 id 查询任务提交历史。
     *
     * @param id 历史记录主键
     */
    @GetMapping("/histories/{id}")
    public R<DzTaskDistListHistoryVo> getHistoryInfo(@NotNull(message = "历史记录主键不能为空")
                                                     @PathVariable Long id) {
        return R.ok(dzTaskDistListService.queryHistoryById(id));
    }

    /**
     * 通过链路 id 查询整条流程链路
     */
    @GetMapping("/process-chain/{chainId}")
    public R<List<TaskProcessChainNodeVo>> processChain(@NotBlank(message = "链路id不能为空")
                                                        @PathVariable("chainId") String chainId) {
        return R.ok(dzTaskDistListService.queryProcessChain(chainId));
    }

    /**
     * 通过链路 id 查询整条流程链路涉及的能力字段。
     */
    @GetMapping("/process-chain/capabilities/{chainId}")
    public R<TaskProcessCapabilityVo> processChainCapabilities(@NotNull(message = "链路id不能为空")
                                                                   @PathVariable String chainId) {
        return R.ok(dzTaskDistListService.queryProcessChainCapabilities(chainId));
    }

    /**
     * 通过链路 id 或任务 id 查询整条流程链路相关任务的提交历史。
     */
    @GetMapping("/process-chain/histories")
    public R<List<DzTaskDistListHistoryVo>> processChainHistories(@RequestParam(value = "chainId", required = false) String chainId,
                                                                  @RequestParam(value = "taskId", required = false) Long taskId) {
        return R.ok(dzTaskDistListService.queryProcessChainHistories(chainId, taskId));
    }

    /**
     * 通过链路 id 查询 AI 分析与推理链。
     */
    @GetMapping("/process-chain/ai/{chainId}")
    public R<TaskAiProcessChainVo> aiProcessChain(@NotBlank(message = "链路id不能为空")
                                                  @PathVariable("chainId") String chainId) {
        return R.ok(dzTaskDistListService.queryAiProcessChain(chainId));
    }

    /**
     * 新增任务派发清单
     */
    @Log(title = "任务派发清单", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody DzTaskDistListBo bo) {
        return toAjax(dzTaskDistListService.insertByBo(bo));
    }

    /**
     * 修改任务派发清单
     */
    @Log(title = "任务派发清单", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody DzTaskDistListBo bo) {
        return toAjax(dzTaskDistListService.updateByBo(bo));
    }

    /**
     * 删除任务派发清单
     *
     * @param ids 主键串
     */
    @Log(title = "任务派发清单", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(dzTaskDistListService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 按链路范围批量删除任务派发清单
     */
    @Log(title = "任务派发清单按链路范围删除", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping("/by-chain-scope")
    public R<TaskDistChainScopeActionResultVo> removeByChainScope(@RequestBody TaskDistChainScopeActionBo bo) {
        return R.ok(dzTaskDistListService.deleteByChainScope(bo));
    }

    /**
     * 统计状态
     */
    @PostMapping("/stat-status")
    public R<List<TaskDistStatStatusVo>> statStatus(@RequestBody TaskDistStatStatusBo bo) {
        List<TaskDistStatStatusVo> vo = dzTaskDistListService.statStatus(bo);
        return R.ok(vo);
    }

    /**
     * 统计来源
     */
    @PostMapping("/stat-source-type")
    public R<List<TaskDistStatSourceTypeVo>> statSourceType(@RequestBody TaskDistStatStatusBo bo) {
        List<TaskDistStatSourceTypeVo> vo = dzTaskDistListService.statSourceType(bo);
        return R.ok(vo);
    }

    /**
     * 巡查规则统计
     */
    @GetMapping("/inspection-rule-stats")
    public R<InspectionRuleStatsVo> inspectionRuleStats(@RequestParam(required = false) String date) {
        return R.ok(dzTaskDistListService.getInspectionRuleStats(parseQueryDate(date)));
    }

    private LocalDate parseQueryDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        String trimmed = date.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new ServiceException("date格式错误，支持yyyy-MM-dd或yyyyMMdd");
    }

    /**
     * 批量新增日常巡查任务
     */
    @PostMapping("/batch-insert-patrol")
    public R<Map<String, Integer>> batchInsertPatrol() {
        return R.ok(dzTaskDistListService.batchInsertPatrol());
    }

    /**
     * 批量新增防御响应任务
     */
    @PostMapping("/batch-insert-def-resp-patrol")
    public R<Integer> batchInsertDefRespPatrol() {
        return R.ok(dzTaskDistListService.batchInsertDefRespPatrol());
    }

    /**
     * 推送
     */
    @Log(title = "任务派发推送", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/push")
    public R<?> push(@RequestBody TaskDistChainScopePushBo bo) {
        Object vo = dzTaskDistListService.pushCompatible(bo);
        return R.ok(vo);
    }

    /**
     * 短信内容预览
     */
    @Log(title = "任务短信预览", businessType = BusinessType.OTHER, isSaveResponseData = false, operatorType = OperatorType.PLATFORM)
    @PostMapping("/push/preview-sms")
    public R<List<TaskDistSmsPreviewItemVo>> previewSms(@RequestBody TaskDistChainScopePushBo bo) {
        return R.ok(dzTaskDistListService.previewSmsCompatible(bo));
    }

    /**
     * 按链路范围推送。
     */
    @Log(title = "任务派发按链路范围推送", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/push/by-chain-scope")
    public R<TaskDistChainPushResultVo> pushByChainScope(@RequestBody TaskDistChainScopePushBo bo) {
        return R.ok(dzTaskDistListService.pushByChainScope(bo));
    }

    /**
     * 按链路范围短信预览。
     */
    @Log(title = "任务派发按链路范围短信预览", businessType = BusinessType.OTHER, isSaveResponseData = false, operatorType = OperatorType.PLATFORM)
    @PostMapping("/push/by-chain-scope/preview-sms")
    public R<TaskDistChainPreviewResultVo> previewSmsByChainScope(@RequestBody TaskDistChainScopePushBo bo) {
        return R.ok(dzTaskDistListService.previewSmsByChainScope(bo));
    }

    /**
     * 催办
     */
    @Log(title = "任务派发催办", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/remind/{id}")
    public R<Void> remind(@PathVariable Long id) {
        dzTaskDistListService.remind(id);
        return R.ok();
    }

    /**
     * 按链路范围批量催办
     */
    @Log(title = "任务派发按链路范围催办", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/remind/by-chain-scope")
    public R<TaskDistChainScopeActionResultVo> remindByChainScope(@RequestBody TaskDistChainScopeActionBo bo) {
        return R.ok(dzTaskDistListService.remindByChainScope(bo));
    }

    /**
     * 统计每日任务数量
     *
     * @param startDateStr 开始日期（格式：YYYY-MM-DD）
     * @param endDateStr   结束日期（格式：YYYY-MM-DD）
     * @return 每日统计列表
     */
    @GetMapping("/stat-day")
    public R<List<TaskDistDayStatVo>> statDay(
        @RequestParam String startDateStr,
        @RequestParam String endDateStr) {
        return R.ok(dzTaskDistListService.statDay(startDateStr, endDateStr));
    }


}
