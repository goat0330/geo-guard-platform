/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import cn.edu.pku.whai.geological.disaster.data.domain.vo.LandPlanningVo;

import java.util.List;

/**
 * 国土空间规划用地类型 Service 接口
 *
 * @author zhuzc
 * @date 2026-06-25
 */
public interface IDataLandPlanningService {

    /**
     * 按经纬度点查询命中的用地类型。
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 命中的用地类型名称；未命中返回 null
     */
    String queryPointLandPlanningType(Double lon, Double lat);

    /**
     * 查询斜坡单元涉及的用地类型统计。
     *
     * @param slopeUnitId 斜坡单元ID
     * @return 用地类型统计列表
     */
    List<LandPlanningVo> querySlopeUnitLandPlanningStatistics(String slopeUnitId);
}
