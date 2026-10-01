/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataGeologicalFaultVo;

/**
 * 地质断层 Service 接口
 *
 * @author zhuzc
 * @date 2026-06-17
 */
public interface IDataGeologicalFaultService {

    /**
     * 按经纬度查询最近的断层。
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 含 distance/azimuth（度）/azimuthName；未命中返回null
     * @throws org.dromara.common.core.exception.ServiceException 经纬度为空或越界
     */
    DataGeologicalFaultVo queryNearestByPoint(Double lon, Double lat);
}
