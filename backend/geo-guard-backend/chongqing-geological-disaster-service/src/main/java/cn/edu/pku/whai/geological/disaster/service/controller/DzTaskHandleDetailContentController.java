/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailContentTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleDetailContentBo;
import cn.edu.pku.whai.geological.disaster.service.domain.req.GenerateEvacuationPlanReq;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleDetailContentVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.EvacuationRouteResultVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzDefRespPlanService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleDetailContentService;
import cn.edu.pku.whai.geological.disaster.service.service.IEvacuationPlanGenerateService;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 处置管理详情内容
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/taskHandleDetail")
public class DzTaskHandleDetailContentController extends BaseController {

    private final IDzTaskHandleDetailContentService dzTaskHandleDetailContentService;
    private final IDzDefRespPlanService defRespPlanService;
    private final IEvacuationPlanGenerateService evacuationPlanGenerateService;

    /**
     * 查询处置管理详情内容列表
     */
    @GetMapping("/list")
    public TableDataInfo<DzTaskHandleDetailContentVo> list(DzTaskHandleDetailContentBo bo, PageQuery pageQuery) {
        validateBizConditionRequired(bo);
        return dzTaskHandleDetailContentService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询符合条件的最新一条处置详情内容
     */
    @GetMapping("/latest")
    public R<DzTaskHandleDetailContentVo> latest(DzTaskHandleDetailContentBo bo) {
        validateBizConditionRequired(bo);
        return R.ok(dzTaskHandleDetailContentService.queryLatest(bo.getBizType(), bo.getBizId(), bo.getContentType(), bo.getRoundNo()));
    }

    /**
     * 将指定详情内容设置为最新
     */
    @Log(title = "处置管理详情内容", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PutMapping("/latest")
    public R<Void> markLatest(@RequestParam @NotNull(message = "id不能为空") Long id) {
        dzTaskHandleDetailContentService.markLatestById(id);
        return R.ok();
    }

    /**
     * 新增处置管理详情内容
     */
    @Log(title = "处置管理详情内容", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody DzTaskHandleDetailContentBo bo) {
        validateBizConditionRequired(bo);
        return toAjax(dzTaskHandleDetailContentService.insertByBo(bo));
    }

    /**
     * 保存处置管理详情内容（防御响应会商确认：同轮次待提交记录存在则更新，否则新增）
     */
    @Log(title = "处置管理详情内容", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PutMapping()
    public R<Long> save(@RequestBody DzTaskHandleDetailContentBo bo) {
        validateBizConditionRequired(bo);
        if (DetailBizTypeEnum.DEF_RESP_PLAN.getCode().equals(bo.getBizType())
            && DetailContentTypeEnum.CONSULTATION_CONFIRM.getCode().equals(bo.getContentType())) {
            return R.ok(defRespPlanService.saveConsultationConfirm(bo));
        }
        if (bo.getId() == null) {
            throw new ServiceException("id不能为空");
        }
        if (!dzTaskHandleDetailContentService.updateByBo(bo)) {
            throw new ServiceException("更新失败");
        }
        return R.ok(bo.getId());
    }

    /**
     * 删除处置管理详情内容
     *
     * @param ids 主键串
     */
    @Log(title = "处置管理详情内容", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(dzTaskHandleDetailContentService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 生成疏散路线
     */
    @Log(title = "疏散路线", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/generateEvacuationRoute")
    public R<EvacuationRouteResultVo> generateEvacuationRoute(@RequestBody @Validated GenerateEvacuationPlanReq req) {
        return R.ok(evacuationPlanGenerateService.generateEvacuationRoute(req.getHandleId()));
    }

    /**
     * 流式生成/修改撤离方案（自动判定当前轮次是否已有方案）
     */
    @Log(title = "撤离方案", businessType = BusinessType.UPDATE, isSaveResponseData = false, operatorType = OperatorType.PLATFORM)
    @PostMapping(value = "/generateEvacuationPlan/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateOrModifyEvacuationPlan(@RequestBody @Validated GenerateEvacuationPlanReq req) {
        SseEmitter sseEmitter = new SseEmitter(0L);
        evacuationPlanGenerateService.generateOrModifyEvacuationPlanStream(sseEmitter, req);
        return sseEmitter;
    }

    private void validateBizConditionRequired(DzTaskHandleDetailContentBo bo) {
        if (bo == null) {
            throw new ServiceException("请求参数不能为空");
        }
        if (bo.getBizType() == null) {
            throw new ServiceException("bizType不能为空");
        }
        if (bo.getContentType() == null) {
            throw new ServiceException("contentType不能为空");
        }
        if (bo.getBizId() == null) {
            throw new ServiceException("bizId不能为空");
        }
    }
}
