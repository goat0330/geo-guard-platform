/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 避难点信息 Mapper。
 */
public interface DzResettlementInfoMapper {

    /**
     * 根据避难点名称查询空间范围 WKT。
     *
     * @param name 避难点名称
     * @return 避难点空间范围 WKT
     */
    @Select("""
        SELECT wkt
        FROM public.dz_resettlement_info
        WHERE name IS NOT NULL
          AND btrim(name) = btrim(#{name})
          AND wkt IS NOT NULL
          AND btrim(wkt) <> ''
        ORDER BY create_time DESC NULLS LAST, id DESC
        LIMIT 1
        """)
    String selectWktByName(@Param("name") String name);
}
