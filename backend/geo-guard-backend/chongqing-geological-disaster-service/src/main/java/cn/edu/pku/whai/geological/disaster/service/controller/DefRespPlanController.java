/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.data.cache.DefenseRespRedisCache;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.DefRespRangeResp;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.*;
import cn.edu.pku.whai.geological.disaster.service.domain.dto.DefRespPlanDto;
import cn.edu.pku.whai.geological.disaster.service.domain.resp.DefRespPlanGenerateByPendingAlarmResp;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.*;
import cn.edu.pku.whai.geological.disaster.service.service.IDzDefRespPlanService;
import cn.edu.pku.whai.geological.disaster.service.task.DefRespScheduledTask;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ObjUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 防御响应方案接口
 *
 * @author whai
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/defRespPlan")
public class DefRespPlanController extends BaseController {


    private final IDzDefRespPlanService defRespPlanService;
    private final DefRespScheduledTask defRespScheduledTask;

    /**
     * 条件查询防御响应方案列表
     */
    @GetMapping("/list")
    public TableDataInfo<DefRespPlanVo> list(DefRespPlanDto bo, PageQuery pageQuery) {
        return defRespPlanService.listByCondition(bo, pageQuery);
    }

    /**
     * 查询当前流转中乡镇级防御响应统计。
     */
    @GetMapping("/circulatingTownStats")
    public R<DefRespTownStatVo> circulatingTownStats(@RequestParam(required = false) Long id) {
        return R.ok(defRespPlanService.getCirculatingTownStats(id));
    }

    /**
     * 树表 根据类型分类计算
     */
    @GetMapping("/tree")
    public R<List<Tree<String>>> getTree() {
        return R.ok(defRespPlanService.getTree());
    }

    /**
     * 获取防御响应方案详细信息
     */
    @GetMapping("/{id}")
    public R<DefRespPlanVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(defRespPlanService.queryById(id));
    }

    /**
     * 生成最新防御响应方案内容
     */
    @Log(title = "防御响应方案", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/generateLatestPlan")
    public R<String> generateLatestPlan(@Validated @RequestBody DefRespPlanGenerateBo bo) {
        return R.ok(defRespPlanService.generateLatestPlanContent(bo));
    }

    /**
     * 手动触发：根据未触发预警生成或更新区域防御响应方案
     */
    @Log(title = "防御响应方案", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/generateByPendingAlarm")
    public R<DefRespPlanGenerateByPendingAlarmResp> generateByPendingAlarm(@RequestBody(required = false) DefRespPlanGenerateByPendingAlarmBo bo) {
        DefRespPlanGenerateByPendingAlarmResp resp = defRespScheduledTask.triggerGenerateDefRespPlan(bo);
        if (resp == null) {
            return R.fail("当前已有防御响应方案生成任务在执行，请稍后重试");
        }
        return R.ok(resp);
    }

    /**
     * 修改防御响应方案
     */
    @Log(title = "防御响应方案", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PutMapping("/edit")
    public R<Map<String, Object>> edit(@Validated(EditGroup.class) @RequestBody DefRespPlanBo bo) {
        return defRespPlanService.updateByBo(bo);
    }

    /**
     * 修改防御响应审批状态。
     */
    @Log(title = "防御响应审批状态", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit
    @PostMapping("/approvalStatus")
    public R<String> approvalStatus(@Validated(EditGroup.class) @RequestBody DefRespPlanApprovalStatusBo bo) {
        return defRespPlanService.updateApprovalStatusByBo(bo);
    }

    /**
     * 推进防御响应方案到下一状态。
     */
    @Log(title = "防御响应方案状态流转", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/processNext/{id}")
    public R<Map<String, Object>> processNext(@NotNull(message = "id不能为空") @PathVariable Long id) {
        return defRespPlanService.processNext(id);
    }

    /**
     * 批量调整乡镇级区域防御响应与县级区域防御响应的关联关系
     */
    @Log(title = "防御响应方案", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/batchRelateTownRegion")
    public R<Integer> batchRelateTownRegion(@Validated @RequestBody List<@Valid DefRespPlanBatchRelateBo> boList) {
        return R.ok(defRespPlanService.batchUpdateTownRegionLevels(boList));
    }

    /**
     * 比对入参与当前流转中乡镇级防御响应等级是否完全一致。
     */
    @PostMapping("/matchCirculatingTownLevels")
    public R<Integer> matchCirculatingTownLevels(@Validated @RequestBody List<@Valid DefRespPlanBatchRelateBo> boList) {
        return R.ok(defRespPlanService.matchCirculatingTownLevels(boList));
    }

    /**
     * 修改防御响应状态(0:未开启，1:已开启)
     */
    @Log(title = "防御响应状态", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/status/{status}")
    public R<Void> status(@PathVariable Integer status) {
        DefenseRespRedisCache.setDefenseRespCache(status);
        return R.ok();
    }

    /**
     * 查询防御响应方案状态(0:未开启，1:已开启)-
     */
    @PostMapping("/getStatus")
    public R<Integer> getStatus() {
        return R.ok(ObjUtil.defaultIfNull(DefenseRespRedisCache.getDefenseRespCache(), 0));
    }

    /**
     * 开启单点防御响应方案
     */
    @Log(title = "防御响应方案", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PostMapping("/startSingle")
    public R<Map<String, Object>> startSingle(String handleId) {
        if (handleId == null) {
            throw new ServiceException("handleId不能为空");
        }
        long handleIdLong;
        try {
            handleIdLong = Long.parseLong(handleId.trim());
        } catch (NumberFormatException e) {
            throw new ServiceException("handleId 格式错误，必须为数字");
        }
        return R.ok(defRespPlanService.startSingle(handleIdLong));
    }


    /**
     * 查看防御响应范围
     */
    @GetMapping("/getDefenseRespRange")
    public R<DefRespRangeResp> getDefenseRespRange() {
        return R.ok(defRespPlanService.getDefenseRespRange());
    }

    /**
     * 按 defId 查询防御响应范围内斜坡单元
     */
    @GetMapping("/rangeSlopeUnits/{defId}")
    public R<List<DefRespPlanRangeSlopeUnitVo>> getRangeSlopeUnits(@NotNull(message = "defId不能为空") @PathVariable Long defId) {
        return R.ok(defRespPlanService.getRangeSlopeUnits(defId));
    }

    /**
     * 查询子节点地象建议。
     * <p>
     * 传入 defId 对应县级区域响应；若该县级响应状态不在 1-5，则查询全部可见县级区域响应下的乡镇子节点。
     */
    @GetMapping("/childGeoAdvice")
    public R<List<DefRespPlanChildGeoAdviceVo>> childGeoAdvice(@RequestParam(required = false) Long alarmId) {
        return R.ok(defRespPlanService.listChildGeoAdvice(alarmId));
    }

    /**
     * 按乡镇/街道列表查询当前等级和地象建议。
     */
    @GetMapping("/streetGeoAdvice")
    public R<List<DefRespPlanStreetGeoAdviceVo>> streetGeoAdvice(@RequestParam List<String> streets) {
        return R.ok(defRespPlanService.queryStreetGeoAdvice(streets));
    }

    /**
     * 查询关联信息
     */
    @GetMapping("/getRelationInfo")
    public R<List<DefRespPlanVo>> getRelationInfo(String id) {
        if (id == null) {
            throw new ServiceException("handleId不能为空");
        }
        long idLong;
        try {
            idLong = Long.parseLong(id.trim());
        } catch (NumberFormatException e) {
            throw new ServiceException("id 格式错误，必须为数字");
        }
        return R.ok(defRespPlanService.getRelationInfo(idLong));
    }

    /**
     * 按指定气象预警同步会商确认内容
     */
    @Log(title = "防御响应会商确认", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/syncConsultationFromAlarm")
    public R<Long> syncConsultationFromAlarm(@Validated @RequestBody DefRespPlanSyncConsultationFromAlarmBo bo) {
        return R.ok(defRespPlanService.syncConsultationFromAlarm(bo.getDefId(), bo.getAlarmId()));
    }

    /**
     * 按 defId 预览启动短信
     */
    @Log(title = "启动短信预览", businessType = BusinessType.OTHER, isSaveResponseData = false, operatorType = OperatorType.PLATFORM)
    @GetMapping("/previewStartSms/{defId}")
    public R<List<DefRespStartSmsPreviewVo>> previewStartSms(@NotNull(message = "defId不能为空") @PathVariable Long defId) {
        return R.ok(defRespPlanService.previewStartSms(defId));
    }

    /**
     * 按 defId 手动触发防御响应聚合短信发送
     */
    @Log(title = "防御响应短信发送", businessType = BusinessType.OTHER, operatorType = OperatorType.PLATFORM)
    @GetMapping("/sendSms/{defId}")
    public R<String> sendSms(@NotNull(message = "defId不能为空") @PathVariable Long defId) {
        SmsSendSummaryVo summary = defRespPlanService.sendStartSms(defId);
        return R.ok("触发完成，成功发送" + summary.getSuccessCount()
            + "人，配置跳过" + summary.getSkipCount()
            + "人，发送失败" + summary.getFailCount() + "人");
    }
}
