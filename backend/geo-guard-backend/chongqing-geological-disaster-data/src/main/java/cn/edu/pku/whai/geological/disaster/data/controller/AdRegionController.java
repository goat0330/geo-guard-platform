/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.AdRegionBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.AdRegionStatBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.AdRegionTownBasicInfoBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionStatVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.service.IAdRegionService;
import cn.hutool.core.lang.tree.Tree;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 行政区划
 *
 * @author kongweiguang
 * @date 2025-12-24
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/adRegion")
public class AdRegionController extends BaseController {

    private final IAdRegionService dataAdRegionService;

    /**
     * 树形结构  查询全部数据
     */
    @GetMapping("tree")
    public R<List<Tree<String>>> tree(@RequestParam(defaultValue = "false") Boolean withWkt,Boolean skipAuth) {
        List<Tree<String>> fr = dataAdRegionService.selectTree(withWkt,skipAuth);
        return R.ok(fr);
    }

    /**
     * 查询行政区划列表
     */
    @GetMapping("list")
    public TableDataInfo<AdRegionVo> list(AdRegionBo bo, PageQuery pageQuery) {
        return dataAdRegionService.queryPageList(bo, pageQuery);
    }

    /**
     * 通过id查询行政区划信息
     */
    @GetMapping("getInfo/{id}")
    public R<AdRegionVo> getInfo(@PathVariable String id) {
        AdRegionVo vo = dataAdRegionService.getInfo(id);
        return R.ok(vo);
    }

    /**
     * 按乡镇名称批量查询乡镇基础信息
     */
    @PostMapping("town/basicInfo")
    public R<List<AdRegionVo>> townBasicInfo(@Valid @RequestBody AdRegionTownBasicInfoBo bo) {
        List<AdRegionVo> vos = dataAdRegionService.queryTownBasicInfoByNames(bo.getTownNames());
        return R.ok(vos);
    }

    /**
     * 获取行政区划信息统计
     */
    @PostMapping("stat")
    public R<AdRegionStatVo> stat(@RequestBody AdRegionStatBo bo) {
        AdRegionStatVo fr = dataAdRegionService.stat(bo);
        return R.ok(fr);
    }

}
