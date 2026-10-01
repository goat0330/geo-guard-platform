/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataWaterSystemGeologyVo;

/**
 * 水系地质 Service
 *
 * @author zhuzc
 * @date 2026-06-04
 */
public interface IWaterSystemGeologyService {

    /**
     * 查询点命中的最近水系
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 最近水系VO；未命中返回null
     */
    DataWaterSystemGeologyVo queryNearestByPoint(Double lon, Double lat);
}
