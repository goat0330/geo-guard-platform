/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.EvacuationPlanBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.EvacuationPlanRouteResultVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.EvacuationPlanVo;
import cn.edu.pku.whai.geological.disaster.data.service.IEvacuationPlanService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 撤离方案表查询接口
 *
 * @author whai
 * @date 2026-02-04
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/evacuationPlan")
public class EvacuationPlanController extends BaseController {

    private final IEvacuationPlanService evacuationPlanService;

    /**
     * 分页查询撤离方案列表
     */
    @GetMapping("/list")
    public TableDataInfo<EvacuationPlanVo> list(EvacuationPlanBo bo, PageQuery pageQuery) {
        return evacuationPlanService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询撤离方案列表（不分页）
     */
    @GetMapping("/listAll")
    public R<List<EvacuationPlanVo>> listAll(EvacuationPlanBo bo) {
        return R.ok(evacuationPlanService.queryList(bo));
    }

    /**
     * 按主键查询撤离方案详情
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<EvacuationPlanVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(evacuationPlanService.queryById(id));
    }

    /**
     * 根据handleId查询当前方案下的所有撤离路线
     *
     * @param handleId 处置主键
     */
    @GetMapping("/routes/{handleId}")
    public R<EvacuationPlanRouteResultVo> getRoutes(@NotNull(message = "handleId不能为空") @PathVariable Long handleId) {
        return R.ok(evacuationPlanService.queryRoutesByHandleId(handleId));
    }
}
