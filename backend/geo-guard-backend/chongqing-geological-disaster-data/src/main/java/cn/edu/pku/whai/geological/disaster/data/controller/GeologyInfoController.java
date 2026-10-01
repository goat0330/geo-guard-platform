/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.GeologyInfoVo;
import cn.edu.pku.whai.geological.disaster.data.service.IGeologyInfoService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 地质信息
 *
 * @author lizheng
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/geology")
public class GeologyInfoController extends BaseController {

    private final IGeologyInfoService geologyInfoService;

    /**
     * 根据点查询地质信息
     */
    @GetMapping("queryByPoint")
    public R<GeologyInfoVo> queryByPoint(@NotNull(message = "经度不能为空") @RequestParam Double lon,
                                         @NotNull(message = "纬度不能为空") @RequestParam Double lat) {
        return R.ok(geologyInfoService.queryByPoint(lon, lat));
    }

    /**
     * 根据区域面查询地质信息
     */
    @GetMapping("queryByBounds")
    public R<GeologyInfoVo> queryByBounds(@NotBlank(message = "wkt不能为空") @RequestParam String wkt) {
        return R.ok(geologyInfoService.queryByBounds(wkt));
    }

    /**
     * 根据行政区名查询地质信息
     */
    @GetMapping("queryByRegions")
    public R<GeologyInfoVo> queryByRegions(@NotBlank(message = "regions不能为空") @RequestParam String regions) {
        return R.ok(geologyInfoService.queryByRegions(regions));
    }

    /**
     * 根据行政区名查询地质信息
     */
    @GetMapping("queryBySlopeUnitId")
    public R<GeologyInfoVo> queryBySlopeUnitId(@NotBlank(message = "id不能为空") @RequestParam String id) {
        return R.ok(geologyInfoService.queryBySlopeUnitId(id));
    }
}
