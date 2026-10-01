/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataGeologicalFoldVo;

/**
 * 地质褶皱 Service 接口
 *
 * @author zhuzc
 * @date 2026-06-17
 */
public interface IDataGeologicalFoldService {

    /**
     * 按经纬度查询最近的褶皱。
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 含 distance/azimuth（度）/azimuthName；未命中返回null
     * @throws org.dromara.common.core.exception.ServiceException 经纬度为空或越界
     */
    DataGeologicalFoldVo queryNearestByPoint(Double lon, Double lat);
}
