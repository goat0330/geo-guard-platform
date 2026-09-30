/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentInfoBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentQueryBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentStatBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.*;
import cn.edu.pku.whai.geological.disaster.service.service.IDzRiskAssessmentService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 斜坡单元风险评估
 *
 * @author kongweiguang
 * @date 2026-01-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/riskAssessment")
public class DzRiskAssessmentController extends BaseController {

    private final IDzRiskAssessmentService dzRiskAssessmentService;

    /**
     * 查询斜坡单元风险评估列表
     */
    @GetMapping("/list")
    public TableDataInfo<DzRiskAssessmentVo> list(DzRiskAssessmentBo bo, PageQuery pageQuery) {
        return dzRiskAssessmentService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询今天临时动态风险等级不为空的风险评估列表
     */
    @GetMapping("/todayTemDynamicRiskList")
    public R<List<DzRiskAssessmentVo>> todayTemDynamicRiskList() {
        return R.ok(dzRiskAssessmentService.queryTodayTemDynamicRiskList());
    }

    /**
     * 查询斜坡单元风险评估详情
     */
    @GetMapping("/{id}")
    public R<DzRiskAssessmentVo> queryById(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        DzRiskAssessmentVo fr = dzRiskAssessmentService.queryById(id);
        return R.ok(fr);
    }

    /**
     * 根据斜坡单元Id和时间查询斜坡单元风险评估详情
     */
    @PostMapping("/unitId")
    public R<DzRiskAssessmentVo> getInfo(@RequestBody DzRiskAssessmentInfoBo bo) {
        DzRiskAssessmentVo fr = dzRiskAssessmentService.getInfo(bo);
        return R.ok(fr);
    }

    /**
     * 统计风险评估数据
     */
    @PostMapping("/stat")
    public R<DzRiskAssessmentStatVo> stat(@RequestBody DzRiskAssessmentStatBo bo) {
        DzRiskAssessmentStatVo fr = dzRiskAssessmentService.stat(bo);
        return R.ok(fr);
    }

    /**
     * 统计指定地区和风险等级的风险评估
     */
    @PostMapping("/statRisk")
    public R<DzRiskAssessmentQueryVo> queryRiskList(@RequestBody DzRiskAssessmentQueryBo bo) {
        DzRiskAssessmentQueryVo vo = dzRiskAssessmentService.queryRiskList(bo);
        return R.ok(vo);
    }

    /**
     * 统计斜坡单元危险性近n天的数据
     */
    @PostMapping("/statHazard")
    public R<List<RiskHazardQueryVo>> statHazard(@RequestBody DzRiskAssessmentStatBo n) {
        List<RiskHazardQueryVo> vo = dzRiskAssessmentService.statHazard(n);
        return R.ok(vo);
    }

    /**
     * 统计聊天横幅数据
     */
    @PostMapping("/statChatBanner")
    public R<RiskChatBannerVo> statChatBanner(@RequestBody DzRiskAssessmentStatBo bo) {
        RiskChatBannerVo vo = dzRiskAssessmentService.statChatBanner(bo);
        return R.ok(vo);
    }

}
