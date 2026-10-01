/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataRoadVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RoadStatisticVo;

import java.util.List;

/**
 * 道路信息Service接口
 *
 * @author system
 * @date 2026-05-15
 */
public interface IDataRoadService {

    /**
     * 根据斜坡单元ID查询道路列表
     *
     * @param slopeUnitId 斜坡单元ID
     * @return 道路列表
     */
    List<DataRoadVo> queryBySlopeUnitId(String slopeUnitId);

    /**
     * 根据范围WKT查询道路列表
     *
     * @param wkt 范围WKT
     * @return 道路列表
     */
    List<DataRoadVo> queryByWkt(String wkt);

    /**
     * 根据斜坡单元ID统计关联道路在斜坡面内的明细（名称、等级、面内长度）。
     * <p>
     * 长度 = ST_Intersection(斜坡面, 道路) 的 ST_Length。
     *
     * @param slopeUnitId  斜坡单元ID
     * @param slopeUnitWkt 斜坡单元范围WKT（用于几何求交）
     * @return 道路面内明细列表
     */
    List<RoadStatisticVo> queryStatisticsBySlopeUnitId(String slopeUnitId, String slopeUnitWkt);

    /**
     * 根据范围WKT统计道路等级与总长度
     *
     * @param wkt 范围WKT
     * @return 道路统计列表
     */
    List<RoadStatisticVo> queryStatisticsByWkt(String wkt);
}
