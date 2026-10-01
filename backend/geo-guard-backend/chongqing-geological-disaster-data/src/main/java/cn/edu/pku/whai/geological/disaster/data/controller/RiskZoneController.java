/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RiskZoneBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RiskZoneVo;
import cn.edu.pku.whai.geological.disaster.data.service.IRiskZoneService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 风险区
 *
 * @author lizheng
 * @date 2026-01-10
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/riskZone")
public class RiskZoneController extends BaseController {

    private final IRiskZoneService dataRiskZoneService;

    /**
     * 查询风险区列表
     */
    @GetMapping("/list")
    public TableDataInfo<RiskZoneVo> list(RiskZoneBo bo, PageQuery pageQuery) {
        return dataRiskZoneService.queryPageList(bo, pageQuery);
    }


    /**
     * 获取风险区详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<RiskZoneVo> getInfo(@NotNull(message = "主键不能为空")
                                 @PathVariable String id) {
        return R.ok(dataRiskZoneService.queryById(id));
    }

}
