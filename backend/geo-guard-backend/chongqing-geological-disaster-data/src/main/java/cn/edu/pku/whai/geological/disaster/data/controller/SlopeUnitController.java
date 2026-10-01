/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.SlopeUnitBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitPersonVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 斜坡单元
 *
 * @author kongweiguang
 * @date 2026-01-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/slopeUnit")
public class SlopeUnitController extends BaseController {

    private final ISlopeUnitService dataSlopeUnitService;

    /**
     * 查询斜坡单元列表
     */
    @GetMapping("/list")
    public TableDataInfo<SlopeUnitVo> list(SlopeUnitBo bo, PageQuery pageQuery) {
        return dataSlopeUnitService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询斜坡单元列表，不包含WKT
     */
    @GetMapping("/list_no_wkt")
    public TableDataInfo<SlopeUnitVo> list_no_wkt(SlopeUnitBo bo, PageQuery pageQuery) {
        return dataSlopeUnitService.queryPageListNoWkt(bo, pageQuery);
    }


    /**
     * 获取斜坡单元详细信息
     *
     * @param id 主键
     */
    @GetMapping("/{id}")
    public R<SlopeUnitVo> getInfo(@NotNull(message = "主键不能为空")
                                  @PathVariable String id) {
        return R.ok(dataSlopeUnitService.queryById(id));
    }


    /**
     * 根据斜坡单元Id查询斜坡单元的负责人
     */
    @GetMapping("/getSlopeUnitPerson/{id}")
    public R<SlopeUnitPersonVo> getSlopeUnitPerson(@NotNull(message = "主键不能为空")
                                             @PathVariable String id) {
        SlopeUnitPersonVo slopeUnitPersonVo = dataSlopeUnitService.getSlopeUnitPerson(id);
        return R.ok(slopeUnitPersonVo);
    }

}
