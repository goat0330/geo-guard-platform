/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.data.domain.po.DataGeomorphological;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataGeomorphologicalVo;
import org.apache.ibatis.annotations.Param;

/**
 * 地貌信息 Mapper
 *
 * @author zhuzc
 * @date 2026-06-11
 */
public interface DataGeomorphologicalMapper extends BaseMapperPlus<DataGeomorphological, DataGeomorphologicalVo> {

    /**
     * 按经纬度点命中包含该点的地貌多边形；命中多条时取中心点最近的 1 条。
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 地貌信息VO；未命中返回 null
     */
    DataGeomorphologicalVo selectByContainingPointNearestCentroid(@Param("lon") Double lon, @Param("lat") Double lat);
}
