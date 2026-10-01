/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import cn.edu.pku.whai.geological.disaster.data.domain.dto.YbssSyfwDto;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataHouseVo;

import java.util.List;

/**
 * 房间信息Service接口
 *
 * @author system
 * @date 2024
 */
public interface IDataHouseService {

    /**
     * 批量保存房间数据
     *
     * @param dataList 房间数据列表
     */
    void saveBatch(List<YbssSyfwDto> dataList);

    /**
     * 根据范围WKT查询房屋列表
     *
     * @param wkt 范围WKT
     * @return 房屋列表
     */
    List<DataHouseVo> queryByWkt(String wkt);

    /**
     * 根据范围WKT按房屋 geometry 查询相交房屋列表
     *
     * @param wkt 范围WKT
     * @return 房屋列表
     */
    List<DataHouseVo> queryByGeometryWkt(String wkt);
}
