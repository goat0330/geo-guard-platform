/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.SlopeUnitBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.req.SlopeUnitReq;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGeologyVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitPersonVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitVo;

import java.util.List;

/**
 * 斜坡单元Service接口
 *
 * @author kongweiguang
 * @date 2026-01-05
 */
public interface ISlopeUnitService {

    /**
     * 查询斜坡单元
     *
     * @param id 主键
     * @return 斜坡单元
     */
    SlopeUnitVo queryById(String id);

    /**
     * 分页查询斜坡单元列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 斜坡单元分页列表
     */
    TableDataInfo<SlopeUnitVo> queryPageList(SlopeUnitBo bo, PageQuery pageQuery);

    List<SlopeUnitVo> querySlopeUnitList(SlopeUnitReq bo);

    SlopeUnitPersonVo getSlopeUnitPerson(String id);

    SlopeUnit queryPoByCenter(String center);

    List<SlopeUnit> listPoByIds(List<String> ids);

    List<SlopeUnitVo> queryNoWktByIds(List<String> ids);

    List<SlopeUnit> listAllWithWkt();

    List<SlopeUnit> listAllWithCenter();

    Long countPilotArea1();

    Long countPilotArea1(SlopeUnitBo bo);

    Long countPilotArea1ByAdRegionIds(SlopeUnitBo bo, List<String> adRegionIds);

    List<SlopeUnitVo> querySlopeUnitListByStreets(List<String> result);

    TableDataInfo<SlopeUnitVo> queryPageListNoWkt(SlopeUnitBo bo, PageQuery pageQuery);

    /**
     * 根据斜坡单元id查询基础信息（data_slope_unit + data_slope_geology 组合视图）
     *
     * @param id 斜坡单元主键
     * @return 组合视图对象；当斜坡单元不存在时返回 null
     */
    SlopeUnitGeologyVo queryGeologyInfoById(String id);

    /**
     * 根据经纬度查询命中的斜坡单元
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 命中的斜坡单元；未命中返回 null
     */
    SlopeUnit queryByPoint(Double lon, Double lat);
}
