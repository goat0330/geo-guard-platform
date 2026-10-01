/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.HouseSlopeBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HouseSlopeGeometryVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HousePointMatchVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HouseSlopeVo;

import java.util.List;

/**
 * 房屋表与边坡单元表空间关联Service接口 v_house_slope
 *
 * @author system
 * @date 2026-01-21
 */
public interface IHouseSlopeService {

    /**
     * 查询房屋表与边坡单元表空间关联
     *
     * @param houseId 房屋ID
     * @return 房屋表与边坡单元表空间关联
     */
    HouseSlopeVo queryById(String houseId);

    /**
     * 分页查询房屋表与边坡单元表空间关联列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 房屋表与边坡单元表空间关联分页列表
     */
    TableDataInfo<HouseSlopeVo> queryPageList(HouseSlopeBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的房屋表与边坡单元表空间关联列表
     *
     * @param bo 查询条件
     * @return 房屋表与边坡单元表空间关联列表
     */
    List<HouseSlopeVo> queryList(HouseSlopeBo bo);

    /**
     * 根据边坡单元ID查询房屋列表
     *
     * @param slopeUnitId 边坡单元ID
     * @return 房屋列表
     */
    List<HouseSlopeVo> queryBySlopeUnitId(String slopeUnitId);

    /**
     * 根据行政区划、斜坡单元查询房屋 geometry 列表。
     *
     * @param adRegionId   行政区划ID，默认恩施市
     * @param slopeUnitId  斜坡单元ID
     * @return geometry 列表
     */
    List<HouseSlopeGeometryVo> queryGeometryList(String adRegionId, String slopeUnitId);

    /**
     * 统计坐标点指定半径范围内（按 building_code 去重）建筑数量。
     *
     * @param lon          经度
     * @param lat          纬度
     * @param radiusMeters 容差半径（米）
     * @return 去重后的建筑数量；未命中返回 0
     */
    Long countByPoint(Double lon, Double lat, Double radiusMeters);

    /**
     * 查询坐标点命中的房屋。先按房屋缓冲区匹配，必要时由调用方改用无缓冲匹配。
     *
     * @param lon          经度
     * @param lat          纬度
     * @param radiusMeters 缓冲半径（米）
     * @param buffered     是否使用缓冲区
     * @return 命中的房屋列表；未命中返回空列表
     */
    List<HousePointMatchVo> queryPointMatches(Double lon, Double lat, Double radiusMeters, boolean buffered);
}
