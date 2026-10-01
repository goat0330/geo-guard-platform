/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;


import cn.edu.pku.whai.geological.disaster.data.domain.vo.GeologyAdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.GeologyInfoVo;

/**
 * 地质信息
 *
 * @author lizheng
 */
public interface IGeologyInfoService {

    GeologyInfoVo queryByPoint(double lon, double lat);

    GeologyAdRegionVo queryAdRegionByPoint(double lon, double lat);

    GeologyInfoVo queryByBounds(String wkt);

    GeologyInfoVo queryByRegions(String regions);

    GeologyInfoVo queryBySlopeUnitId(String id);

    /**
     * 根据斜坡单元WKT查询地质信息（仅工程地质/水文地质/地层，不含房屋/人数）
     *
     * @param wkt 斜坡单元WKT（POLYGON）
     * @return 地质信息VO
     */
    GeologyInfoVo queryBySlopeUnitWkt(String wkt);
}
