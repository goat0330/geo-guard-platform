/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.HouseSlopeBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HouseSlopeVo;
import cn.edu.pku.whai.geological.disaster.data.service.IHouseSlopeService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 房屋与边坡单元空间关联
 *
 * @author system
 * @date 2026-05-14
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/houseSlope")
public class HouseSlopeController extends BaseController {

    private final IHouseSlopeService houseSlopeService;

    /**
     * 查询房屋与边坡单元空间关联列表
     */
    @GetMapping("/list")
    public TableDataInfo<HouseSlopeVo> list(HouseSlopeBo bo, PageQuery pageQuery) {
        return houseSlopeService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取房屋与边坡单元空间关联详细信息
     *
     * @param id 房屋ID
     */
    @GetMapping("/{id}")
    public R<HouseSlopeVo> getInfo(@NotBlank(message = "房屋ID不能为空")
                                   @PathVariable String id) {
        return R.ok(houseSlopeService.queryById(id));
    }

    /**
     * 根据边坡单元ID查询房屋列表
     *
     * @param slopeUnitId 边坡单元ID
     */
    @GetMapping("/bySlopeUnitId/{slopeUnitId}")
    public R<List<HouseSlopeVo>> listBySlopeUnitId(
        @NotBlank(message = "边坡单元ID不能为空") @PathVariable String slopeUnitId) {
        return R.ok(houseSlopeService.queryBySlopeUnitId(slopeUnitId));
    }
}
