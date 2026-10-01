/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import cn.edu.pku.whai.geological.disaster.data.domain.bo.RainfallLogSlopeUnitStatisticQueryBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.RainfallEntityGridMapping;

import java.util.List;
import java.util.Set;

/**
 * 雨量统计对象与网格映射 Service。
 *
 * @author system
 * @date 2026-04-22
 */
public interface IRainfallEntityGridMappingService {

    /**
     * 保存或更新对象与网格的映射关系。
     *
     * @param mappings 映射列表
     */
    void saveOrUpdateMappings(List<RainfallEntityGridMapping> mappings);

    /**
     * 查询斜坡单元对应的网格映射。
     *
     * @param slopeUnitId 斜坡单元ID
     * @return 映射
     */
    RainfallEntityGridMapping querySlopeUnitMapping(String slopeUnitId);

    /**
     * 查询全部斜坡单元映射。
     *
     * @return 映射列表
     */
    List<RainfallEntityGridMapping> listSlopeUnitMappings();

    /**
     * 按行政区划条件查询映射。
     *
     * @param bo 查询条件
     * @return 映射列表
     */
    List<RainfallEntityGridMapping> listAdRegionMappings(RainfallLogSlopeUnitStatisticQueryBo bo);

    /**
     * 查询恩施市街道/乡镇映射。
     *
     * @return 映射列表
     */
    List<RainfallEntityGridMapping> listEnshiStreetMappings();

    /**
     * 查询映射表中所有不重复的网格经纬度键（格式：lon_lat）。
     *
     * @return 网格键集合
     */
    Set<String> listDistinctGridKeys();

    /**
     * 查询映射表中所有不重复的预报网格经纬度键（格式：lon_lat）。
     *
     * @return 预报网格键集合
     */
    Set<String> listDistinctForecastGridKeys();
}
