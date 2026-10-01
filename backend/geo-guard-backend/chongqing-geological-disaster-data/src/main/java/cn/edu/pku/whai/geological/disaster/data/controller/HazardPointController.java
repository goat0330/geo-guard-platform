/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.HazardPointBo;
import cn.edu.pku.whai.geological.disaster.data.domain.req.AreaWktReq;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HazardPointVo;
import cn.edu.pku.whai.geological.disaster.data.service.IHazardPointService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 隐患点基本情况
 *
 * @author kongweiguang
 * @date 2026-01-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/hazardPoint")
public class HazardPointController extends BaseController {

    private final IHazardPointService dataHazardPointService;

    /**
     * 查询隐患点基本情况列表
     */
    @GetMapping("/list")
    public TableDataInfo<HazardPointVo> list(HazardPointBo bo, PageQuery pageQuery) {
        return dataHazardPointService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取隐患点基本情况详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<HazardPointVo> getInfo(@NotNull(message = "主键不能为空")
                                    @PathVariable String id) {
        return R.ok(dataHazardPointService.queryById(id));
    }

    /**
     * 根据范围WKT查询隐患点列表
     */
    @PostMapping("/queryByWkt")
    public R<List<HazardPointVo>> queryByWkt(@Valid @RequestBody AreaWktReq req) {
        return R.ok(dataHazardPointService.queryByWkt(req.getWkt()));
    }

}
