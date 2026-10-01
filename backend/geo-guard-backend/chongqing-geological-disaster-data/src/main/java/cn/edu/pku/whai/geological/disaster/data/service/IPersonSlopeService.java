/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.PersonSlopeBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.PersonSlopeVo;

import java.util.List;

/**
 * 人员表与边坡单元表空间关联Service接口 v_person_slope
 *
 * @author system
 * @date 2026-01-21
 */
public interface IPersonSlopeService {

    /**
     * 查询人员表与边坡单元表空间关联
     *
     * @param personId 人员ID
     * @return 人员表与边坡单元表空间关联
     */
    PersonSlopeVo queryById(String personId);

    /**
     * 分页查询人员表与边坡单元表空间关联列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 人员表与边坡单元表空间关联分页列表
     */
    TableDataInfo<PersonSlopeVo> queryPageList(PersonSlopeBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的人员表与边坡单元表空间关联列表
     *
     * @param bo 查询条件
     * @return 人员表与边坡单元表空间关联列表
     */
    List<PersonSlopeVo> queryList(PersonSlopeBo bo);

    /**
     * 根据边坡单元ID查询人员列表
     *
     * @param slopeUnitId 边坡单元ID
     * @return 人员列表
     */
    List<PersonSlopeVo> queryBySlopeUnitId(String slopeUnitId);

    /**
     * 统计坐标点指定半径范围内的人口数量。
     *
     * @param lon          经度
     * @param lat          纬度
     * @param radiusMeters 容差半径（米）
     * @return 人口数量；未命中返回 0
     */
    Long countByPoint(Double lon, Double lat, Double radiusMeters);

    /**
     * 根据房屋关联键统计人口。优先使用 buildingCode，缺失时使用 houseUnitId。
     *
     * @param houseUnitId  户室唯一ID
     * @param buildingCode 房屋建筑代码
     * @return 人口数量；未命中返回 0
     */
    Long countByHouse(String houseUnitId, String buildingCode);

    /**
     * 根据房屋关联键统计户数。优先使用 buildingCode，缺失时使用 houseUnitId。
     *
     * @param houseUnitId  户室唯一ID
     * @param buildingCode 房屋建筑代码
     * @return 户数；未命中返回 0
     */
    Long countHouseholdByHouse(String houseUnitId, String buildingCode);
}
