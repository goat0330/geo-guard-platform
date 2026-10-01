/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;


import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.AdRegionBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.AdRegionStatBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionStatVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.hutool.core.lang.tree.Tree;

import java.util.List;

/**
 * 行政区划 Service接口
 *
 * @author kongweiguang
 * @date 2025-12-24
 */
public interface IAdRegionService {
    /**
     * 分页查询【请填写功能名称】列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 【请填写功能名称】分页列表
     */
    TableDataInfo<AdRegionVo> queryPageList(AdRegionBo bo, PageQuery pageQuery);

    List<Tree<String>> selectTree(Boolean withWkt, Boolean skipAuth);

    AdRegionStatVo stat(AdRegionStatBo bo);

    AdRegionVo getInfo(String id);

    List<AdRegionVo> queryTownBasicInfoByNames(List<String> townNames);

    List<AdRegionVo> querySlopeUnitListByStreets(List<String> streets);

    /**
     * 通过用户id获取用户辖区
     */
    AdRegionVo getUserRegion(Long userId);

    /**
     * 条件查询行政区划列表
     */
    List<AdRegionVo> queryList(AdRegionBo bo);
}
