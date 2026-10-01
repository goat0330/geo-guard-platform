/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.data.domain.req.DataRoadWktReq;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataRoadVo;
import cn.edu.pku.whai.geological.disaster.data.service.IDataRoadService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
 * 道路数据查询Controller
 *
 * @author system
 * @date 2026-05-15
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/dataRoad")
public class DataRoadController extends BaseController {

    private final IDataRoadService dataRoadService;

    /**
     * 根据斜坡单元ID查询道路列表
     *
     * @param slopeUnitId 斜坡单元ID
     * @return 道路列表
     */
    @GetMapping("/bySlopeUnitId/{slopeUnitId}")
    public R<List<DataRoadVo>> queryBySlopeUnitId(
        @NotBlank(message = "斜坡单元ID不能为空") @PathVariable String slopeUnitId) {
        return R.ok(dataRoadService.queryBySlopeUnitId(slopeUnitId));
    }

    /**
     * 根据范围WKT查询道路列表
     *
     * @param req 范围WKT
     * @return 道路列表
     */
    @PostMapping("/queryByWkt")
    public R<List<DataRoadVo>> queryByWkt(@Valid @RequestBody DataRoadWktReq req) {
        return R.ok(dataRoadService.queryByWkt(req.getWkt()));
    }
}
