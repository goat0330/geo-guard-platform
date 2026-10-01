/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.data.domain.po.DataGeologicalFault;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataGeologicalFaultVo;
import org.apache.ibatis.annotations.Param;

/**
 * 地质断层 Mapper
 *
 * @author zhuzc
 * @date 2026-06-17
 */
public interface DataGeologicalFaultMapper extends BaseMapperPlus<DataGeologicalFault, DataGeologicalFaultVo> {

    /**
     * 按经纬度查询最近的断层（取首条，GIST KNN）。
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 最近断层VO（含 distance/azimuth 弧度）；未命中返回null
     */
    DataGeologicalFaultVo selectNearestByPoint(@Param("lon") Double lon, @Param("lat") Double lat);
}
