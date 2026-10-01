/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import cn.edu.pku.whai.geological.disaster.data.domain.dto.YbssSyrkDto;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.BuildingPopulationProfileVo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 人员信息Service接口
 *
 * @author system
 * @date 2024
 */
public interface IDataPersonService {

    /**
     * 批量保存人员数据
     *
     * @param dataList 人员数据列表
     */
    void saveBatch(List<YbssSyrkDto> dataList);

    /**
     * 根据经纬度范围统计人员数量
     *
     * @param lonMax 最大经度
     * @param latMax 最大纬度
     * @param lonMin 最小经度
     * @param latMin 最小纬度
     * @return 范围内人员数量
     */
    long countByScope(BigDecimal lonMax, BigDecimal latMax, BigDecimal lonMin, BigDecimal latMin);

    /**
     * 根据经纬度范围查询人员电话列表
     *
     * @param lonMax 最大经度
     * @param latMax 最大纬度
     * @param lonMin 最小经度
     * @param latMin 最小纬度
     * @return 范围内人员的联系电话列表（去重、过滤空值）
     */
    List<String> listPhoneByScope(BigDecimal lonMax, BigDecimal latMax, BigDecimal lonMin, BigDecimal latMin);

    /**
     * 根据区域WKT查询人员电话列表
     *
     * @param polygonWkt 区域WKT
     * @return 范围内人员的联系电话列表（去重、过滤空值）
     */
    List<String> listPhoneByPolygonWkt(String polygonWkt);

    /**
     * 根据撤离区域名称匹配建筑名称，再按建筑经纬度反查人员电话列表。
     *
     * @param evacuationArea 撤离区域名称
     * @return 匹配到的联系电话列表（去重、过滤空值）
     */
    List<String> listPhoneByEvacuationArea(String evacuationArea);

    /**
     * 根据经纬度范围统计户数（按户室唯一ID去重）
     *
     * @param lonMax 最大经度
     * @param latMax 最大纬度
     * @param lonMin 最小经度
     * @param latMin 最小纬度
     * @return 范围内户数
     */
    long countHouseholdByScope(BigDecimal lonMax, BigDecimal latMax, BigDecimal lonMin, BigDecimal latMin);

    /**
     * 根据建筑编码统计人员数量。
     *
     * @param buildingCode 建筑编码
     * @return 人员数量
     */
    long countByBuildingCode(String buildingCode);

    /**
     * 根据区域WKT统计人员数量
     *
     * @param polygonWkt 区域WKT
     * @return 人员数量
     */
    long countByPolygonWkt(String polygonWkt);

    /**
     * 根据区域WKT统计户数（按户室唯一ID去重）
     *
     * @param polygonWkt 区域WKT
     * @return 户数
     */
    long countHouseholdByPolygonWkt(String polygonWkt);

    /**
     * 按试点区/示范区及行政区划统计人口、房屋数量
     *
     * @param areaType     区域类型：1-试点区，2-示范区，null-不限制
     * @param provinceCode 省编码
     * @param cityCode     市编码
     * @param countyCode   区县编码
     * @param streetCode   乡镇街编码
     * @param villageCode  村编码
     * @return 统计结果
     */
    Map<String, Object> statPopulationAndHouse(Integer areaType, String provinceCode, String cityCode, String countyCode,
                                               String streetCode, String villageCode);

    /**
     * 根据中心点查询最近命中的建筑及其人口画像。
     *
     * @param longitude 中心点经度
     * @param latitude  中心点纬度
     * @return 建筑人口画像
     */
    BuildingPopulationProfileVo getBuildingPopulationProfileByPoint(BigDecimal longitude, BigDecimal latitude, Integer radius);
}
