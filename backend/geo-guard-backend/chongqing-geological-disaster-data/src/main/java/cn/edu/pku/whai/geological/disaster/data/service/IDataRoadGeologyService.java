/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataRoadGeologyVo;

/**
 * 道路地质 Service
 *
 * @author zhuzc
 * @date 2026-06-04
 */
public interface IDataRoadGeologyService {

    /**
     * 查询点命中的最近道路
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 最近道路VO；未命中返回null
     */
    DataRoadGeologyVo queryNearestByPoint(Double lon, Double lat);
}
