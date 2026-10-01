/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataGeomorphologicalVo;

/**
 * 地貌信息 Service 接口
 *
 * @author zhuzc
 * @date 2026-06-11
 */
public interface IDataGeomorphologicalService {

    /**
     * 按经纬度点查询命中的地貌信息；命中多条时取中心点最近的 1 条。
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 命中的地貌VO；未命中返回 null；查询异常返回 null
     * @throws ServiceException 经纬度为空或范围非法
     */
    DataGeomorphologicalVo queryByContainingPointNearestCentroid(Double lon, Double lat);
}
