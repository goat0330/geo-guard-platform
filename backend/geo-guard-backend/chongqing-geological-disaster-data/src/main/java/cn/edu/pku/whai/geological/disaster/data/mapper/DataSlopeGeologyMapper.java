/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.data.domain.po.DataSlopeGeology;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGeologyVo;
import org.apache.ibatis.annotations.Param;

/**
 * 斜坡单元地质信息Mapper接口
 *
 * @author zhuzc
 */
public interface DataSlopeGeologyMapper extends BaseMapperPlus<DataSlopeGeology, DataSlopeGeology> {

    /**
     * 根据斜坡单元id单次查询基础信息（data_slope_unit LEFT JOIN data_slope_geology）
     *
     * @param id 斜坡单元主键
     * @return 组合视图对象；斜坡单元不存在时返回 null
     */
    SlopeUnitGeologyVo selectBaseInfoById(@Param("id") String id);

}
