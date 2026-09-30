/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzMsgNoticeBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzMsgNoticeVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.MsgNoticeCountStat;
import cn.edu.pku.whai.geological.disaster.service.service.IDzMsgNoticeService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业务消息通知
 *
 * @author kongweiguang
 * @date 2026-04-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/msgNotice")
public class DzMsgNoticeController extends BaseController {

    private final IDzMsgNoticeService dzMsgNoticeService;

    /**
     * 查询业务消息通知列表
     */
    @GetMapping("/list")
    public TableDataInfo<DzMsgNoticeVo> list(DzMsgNoticeBo bo, PageQuery pageQuery) {
        bo.setUserId(LoginHelper.getUserId());
        return dzMsgNoticeService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出业务消息通知列表
     */
    @Log(title = "业务消息通知", businessType = BusinessType.EXPORT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/export")
    public void export(DzMsgNoticeBo bo, HttpServletResponse response) {
        List<DzMsgNoticeVo> list = dzMsgNoticeService.queryList(bo);
        ExcelUtil.exportExcel(list, "业务消息通知", DzMsgNoticeVo.class, response);
    }

    /**
     * 获取业务消息通知详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<DzMsgNoticeVo> getInfo(@NotNull(message = "主键不能为空")
                                    @PathVariable Long id) {
        return R.ok(dzMsgNoticeService.queryById(id));
    }

    /**
     * 新增业务消息通知
     */
    @Log(title = "业务消息通知", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody DzMsgNoticeBo bo) {
        return toAjax(dzMsgNoticeService.insertByBo(bo));
    }

    /**
     * 修改业务消息通知
     */
    @Log(title = "业务消息通知", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody DzMsgNoticeBo bo) {
        return toAjax(dzMsgNoticeService.updateByBo(bo));
    }

    /**
     * 删除业务消息通知
     *
     * @param ids 主键串
     */
    @Log(title = "业务消息通知", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(dzMsgNoticeService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 处理业务消息通知
     */
    @Log(title = "业务消息通知", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PostMapping("/handle")
    public R<Void> handle( @RequestBody DzMsgNoticeBo bo) {
        dzMsgNoticeService.handle(bo);
        return R.ok();
    }

    /**
     * 统计业务消息通知状态
     */
    @GetMapping("/countStat")
    public R<MsgNoticeCountStat> countStatus() {
        return R.ok(dzMsgNoticeService.countStatus(LoginHelper.getUserId()));
    }
}
