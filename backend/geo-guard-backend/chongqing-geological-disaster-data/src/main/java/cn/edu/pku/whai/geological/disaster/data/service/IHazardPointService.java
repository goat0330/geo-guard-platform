/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;


import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.HazardPointBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HazardPointVo;

import java.util.List;

/**
 * 隐患点基本情况Service接口
 *
 * @author kongweiguang
 * @date 2026-01-05
 */
public interface IHazardPointService {

    /**
     * 查询隐患点基本情况
     *
     * @param id 主键
     * @return 隐患点基本情况
     */
    HazardPointVo queryById(String id);

    /**
     * 分页查询隐患点基本情况列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 隐患点基本情况分页列表
     */
    TableDataInfo<HazardPointVo> queryPageList(HazardPointBo bo, PageQuery pageQuery);

    List<HazardPointVo> queryList(HazardPointBo bo);

    /**
     * 根据范围WKT查询隐患点列表
     *
     * @param wkt 范围WKT
     * @return 隐患点列表
     */
    List<HazardPointVo> queryByWkt(String wkt);


    /**
     * 从接口同步数据
     */
    void syncData();

    List<HazardPointVo> queryBySlopeUnitId(String id, String area);

    Long countAll();

    /**
     * 按斜坡单元ID集合统计隐患点数量。
     *
     * @param slopeUnitIds 斜坡单元ID集合
     * @return 隐患点数量
     */
    Long countBySlopeUnitIds(List<String> slopeUnitIds);

    /**
     * 按经纬度点匹配隐患点范围，命中多条时取中心点最近的 1 条。
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 最近一条隐患点VO（含 geologicalEnvironment）；未命中返回 null
     */
    HazardPointVo queryByContainingPointNearestCenter(Double lon, Double lat);
}
