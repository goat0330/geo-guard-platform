package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleApprovalBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleApprovalVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzDefRespPlanService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleApprovalService;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务处置审批
 *
 * @author kongweiguang
 * @date 2026-02-06
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/taskHandleApproval")
public class DzTaskHandleApprovalController extends BaseController {

    private final IDzTaskHandleApprovalService dzTaskHandleApprovalService;
    private final IDzDefRespPlanService dzDefRespPlanService;

    /**
     * 查询任务处置审批列表
     */
    @GetMapping("/list")
    public TableDataInfo<DzTaskHandleApprovalVo> list(DzTaskHandleApprovalBo bo, PageQuery pageQuery) {
        return dzTaskHandleApprovalService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询任务处置审批状态列表
     */
    @GetMapping("/status")
    public R<List<DzTaskHandleApprovalVo>> listStatus(String handleId, Integer roundNo) {
        long handleIdLong;
        try {
            handleIdLong = Long.parseLong(handleId.trim());
        } catch (NumberFormatException e) {
            throw new ServiceException("handleId 格式错误，必须为数字");
        }
        return R.ok(dzTaskHandleApprovalService.listStatus(handleIdLong, roundNo));
    }

    /**
     * 获取任务处置审批详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<DzTaskHandleApprovalVo> getInfo(@NotNull(message = "主键不能为空")
                                             @PathVariable Long id) {
        return R.ok(dzTaskHandleApprovalService.queryById(id));
    }

    /**
     * 新增任务处置审批
     */
    @Log(title = "任务处置审批", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody DzTaskHandleApprovalBo bo) {
        bo.setUserId(LoginHelper.getUserId());
        return toAjax(dzTaskHandleApprovalService.insertByBo(bo));
    }

    /**
     * 新增任务处置行政人员审批
     */
    @Log(title = "任务处置行政人员审批", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/executive")
    public R<String> addExecutive(@NotNull(message = "handleId不能为空") String handleId, @NotNull(message = "meetingType不能为空") Integer meetingType, @NotNull(message = "handleProcess不能为空") Integer handleProcess) {
        long handleIdLong;
        try {
            handleIdLong = Long.parseLong(handleId.trim());
        } catch (NumberFormatException e) {
            throw new ServiceException("handleId 格式错误，必须为数字");
        }
        return dzDefRespPlanService.insertExecutiveByBo(handleIdLong, meetingType, handleProcess, null);
    }

    /**
     * 修改任务处置审批
     */
    @Log(title = "任务处置审批", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody DzTaskHandleApprovalBo bo) {
        bo.setUserId(LoginHelper.getUserId());
        return toAjax(dzTaskHandleApprovalService.updateByBo(bo));
    }

    /**
     * 删除任务处置审批
     *
     * @param ids 主键串
     */
    @Log(title = "任务处置审批", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(dzTaskHandleApprovalService.deleteWithValidByIds(List.of(ids), true));
    }
}
