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
import cn.edu.pku.whai.geological.disaster.service.domain.bo.AiModifyReportBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.GenerateReviewReportBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.ModifyReportBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskOverviewVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleRiskAreaStatVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.EvacuationSmsSendWrapResult;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.HandleStatVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 灾害处置
 *
 * @author kongweiguang
 * @date 2026-01-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/taskHandle")
public class DzTaskHandleController extends BaseController {

    private final IDzTaskHandleService dzTaskHandleService;

    /**
     * 查询灾害处置列表
     */
    @GetMapping("/list")
    public TableDataInfo<DzTaskHandleVo> list(DzTaskHandleBo bo, PageQuery pageQuery) {
        return dzTaskHandleService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出灾害处置列表
     */
    @Log(title = "灾害处置", businessType = BusinessType.EXPORT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/export")
    public void export(DzTaskHandleBo bo, HttpServletResponse response) {
        List<DzTaskHandleVo> list = dzTaskHandleService.queryList(bo);
        ExcelUtil.exportExcel(list, "灾害处置", DzTaskHandleVo.class, response);
    }

    /**
     * 获取灾害处置详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<DzTaskHandleVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(dzTaskHandleService.queryById(id));
    }

    /**
     * 新增灾害处置
     */
    @Log(title = "灾害处置", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody DzTaskHandleBo bo) {
        return toAjax(dzTaskHandleService.insertByBo(bo));
    }

    /**
     * 修改灾害处置
     */
    @Log(title = "灾害处置", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody DzTaskHandleBo bo) {
        return toAjax(dzTaskHandleService.updateByBo(bo));
    }

    /**
     * 删除灾害处置
     *
     * @param ids 主键串
     */
    @Log(title = "灾害处置", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(dzTaskHandleService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * ai处理报告，没有报告则生成，有报告根据prompt修改报告
     */
    @Log(title = "灾害处置报告", businessType = BusinessType.UPDATE, isSaveResponseData = false, operatorType = OperatorType.PLATFORM)
    @PostMapping("/aiModifyReportStream")
    public SseEmitter aiModifyReportStream(@RequestBody AiModifyReportBo bo) {
        SseEmitter sseEmitter = new SseEmitter();
        dzTaskHandleService.aiModifyReportStream(sseEmitter, bo);
        return sseEmitter;
    }


    /**
     * ai处理报告，没有报告则生成，有报告根据prompt修改报告
     */
    @Log(title = "灾害处置报告", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/aiModifyReport")
    public R<String> aiModifyReport(@RequestBody AiModifyReportBo bo) {
        String report = dzTaskHandleService.aiModifyReport(bo);
        return R.ok(report);
    }

    /**
     * 修改处置报告
     */
    @Log(title = "灾害处置报告", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/modifyReport")
    public R<Void> modifyReport(@RequestBody ModifyReportBo bo) {
        dzTaskHandleService.modifyReport(bo);
        return R.ok();
    }

    /**
     * 生成复盘报告
     */
    @Log(title = "灾害处置复盘报告", businessType = BusinessType.INSERT, isSaveResponseData = false, operatorType = OperatorType.PLATFORM)
    @PostMapping("/generateReviewReportStream")
    public SseEmitter generateReviewReportStream(@Validated @RequestBody GenerateReviewReportBo bo) {
        SseEmitter sseEmitter = new SseEmitter();
        dzTaskHandleService.generateReviewReportStream(sseEmitter, bo);
        return sseEmitter;
    }

    /**
     * 根据方案Id自动生成任务,并批量发送短信,生成具体的短信任务表
     */
    @Log(title = "灾害处置任务执行", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/execute")
    public R<Void> execute(String handleId) {
        if (handleId == null) {
            throw new ServiceException("handleId不能为空");
        }
        long handleIdLong;
        try {
            handleIdLong = Long.parseLong(handleId.trim());
        } catch (NumberFormatException e) {
            throw new ServiceException("handleId 格式错误，必须为数字");
        }
        return toAjax(dzTaskHandleService.execute(handleIdLong));
    }

    /**
     * 进行下一步流程
     */
    @Log(title = "灾害处置流程", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/processNext/{id}")
    public R<Integer> processNext(@PathVariable Long id) {
        Integer result = dzTaskHandleService.processNext(id);
        return R.ok(result);
    }


    /**
     * 统计处置数据
     */
    @PostMapping("/stat")
    public R<HandleStatVo> stat() {
        HandleStatVo vo = dzTaskHandleService.stat();
        return R.ok(vo);
    }

    /**
     * 仅发布处置任务
     */
    @Log(title = "灾害处置任务发布", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/publish/skip/{id}")
    public R<Integer> skip(@PathVariable Long id) {
        return R.ok(dzTaskHandleService.skip(id));
    }

    /**
     * 根据用户关联区域,展示实时风险概况
     *
     * @param type 1: 应急处置, 2: 防御响应
     */
    @GetMapping("/riskOverview")
    public R<DzRiskOverviewVo> riskOverview(@NotNull(message = "类型不能为空") Integer type) {
        DzRiskOverviewVo vo = dzTaskHandleService.riskOverview(type);
        return R.ok(vo);
    }

    /**
     * 查询当前所有执行中的处置事件
     */
    @GetMapping("/getAllHandling")
    public R<List<DzTaskHandleVo>> getAllHandling() {
        List<DzTaskHandleVo> list = dzTaskHandleService.getAllHandling();
        return R.ok(list);
    }

    /**
     * 按 handleId 查看处置方案响应执行进度
     *
     * @param handleId 处置与撤离方案主键
     */
    @GetMapping("/status")
    public R<EvacuationSmsSendWrapResult> status(@RequestParam("handleId") @NotNull(message = "handleId不能为空") String handleId) {
        long handleIdLong;
        try {
            handleIdLong = Long.parseLong(handleId.trim());
        } catch (NumberFormatException e) {
            throw new ServiceException("handleId 格式错误，必须为数字");
        }
        return R.ok(dzTaskHandleService.status(handleIdLong));
    }

    /**
     * 按 handleId 统计风险区域内建筑与人口
     */
    @GetMapping("/riskAreaStat")
    public R<DzTaskHandleRiskAreaStatVo> riskAreaStat(@RequestParam("handleId") @NotNull(message = "handleId不能为空") String handleId) {
        long handleIdLong;
        try {
            handleIdLong = Long.parseLong(handleId.trim());
        } catch (NumberFormatException e) {
            throw new ServiceException("handleId 格式错误，必须为数字");
        }
        return R.ok(dzTaskHandleService.riskAreaStat(handleIdLong));
    }
}
