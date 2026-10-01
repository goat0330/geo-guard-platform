/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.data.domain.po.DataGeologicalFold;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataGeologicalFoldVo;
import org.apache.ibatis.annotations.Param;

/**
 * 地质褶皱 Mapper
 *
 * @author zhuzc
 * @date 2026-06-17
 */
public interface DataGeologicalFoldMapper extends BaseMapperPlus<DataGeologicalFold, DataGeologicalFoldVo> {

    /**
     * 按经纬度查询最近的褶皱（取首条，GIST KNN）。
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 最近褶皱VO（含 distance/azimuth 弧度）；未命中返回null
     */
    DataGeologicalFoldVo selectNearestByPoint(@Param("lon") Double lon, @Param("lat") Double lat);
}
