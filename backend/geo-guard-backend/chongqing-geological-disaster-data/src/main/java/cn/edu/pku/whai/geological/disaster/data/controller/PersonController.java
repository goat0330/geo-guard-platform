/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.BuildingPopulationProfileVo;
import cn.edu.pku.whai.geological.disaster.data.service.IDataPersonService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 人员
 *
 * @author 12064
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/person")
public class PersonController extends BaseController {

    private final IDataPersonService dataPersonService;

    /**
     * 根据经纬度范围查询人员数量
     *
     * @param lonMax 最大经度
     * @param latMax 最大纬度
     * @param lonMin 最小经度
     * @param latMin 最小纬度
     */
    @GetMapping("/scope/count")
    public R<Long> countByScope(
        @RequestParam("lonMax") @NotNull(message = "最大经度不能为空") BigDecimal lonMax,
        @RequestParam("latMax") @NotNull(message = "最大纬度不能为空") BigDecimal latMax,
        @RequestParam("lonMin") @NotNull(message = "最小经度不能为空") BigDecimal lonMin,
        @RequestParam("latMin") @NotNull(message = "最小纬度不能为空") BigDecimal latMin) {
        return R.ok(dataPersonService.countByScope(lonMax, latMax, lonMin, latMin));
    }

    /**
     * 根据经纬度范围统计户数（按户室唯一ID去重）
     *
     * @param lonMax 最大经度
     * @param latMax 最大纬度
     * @param lonMin 最小经度
     * @param latMin 最小纬度
     */
    @GetMapping("/scope/householdCount")
    public R<Long> countHouseholdByScope(
        @RequestParam("lonMax") @NotNull(message = "最大经度不能为空") BigDecimal lonMax,
        @RequestParam("latMax") @NotNull(message = "最大纬度不能为空") BigDecimal latMax,
        @RequestParam("lonMin") @NotNull(message = "最小经度不能为空") BigDecimal lonMin,
        @RequestParam("latMin") @NotNull(message = "最小纬度不能为空") BigDecimal latMin) {
        return R.ok(dataPersonService.countHouseholdByScope(lonMax, latMax, lonMin, latMin));
    }

    /**
     * 根据经纬度范围查询人员电话列表
     *
     * @param lonMax 最大经度
     * @param latMax 最大纬度
     * @param lonMin 最小经度
     * @param latMin 最小纬度
     */
    @GetMapping("/scope/phones")
    public R<List<String>> listPhonesByScope(
        @RequestParam("lonMax") @NotNull(message = "最大经度不能为空") BigDecimal lonMax,
        @RequestParam("latMax") @NotNull(message = "最大纬度不能为空") BigDecimal latMax,
        @RequestParam("lonMin") @NotNull(message = "最小经度不能为空") BigDecimal lonMin,
        @RequestParam("latMin") @NotNull(message = "最小纬度不能为空") BigDecimal latMin) {
        return R.ok(dataPersonService.listPhoneByScope(lonMax, latMax, lonMin, latMin));
    }

    /**
     * 按试点区/示范区及行政区划统计人口、房屋数量
     *
     * @param areaType     区域类型：1-试点区，2-示范区
     * @param provinceCode 省编码
     * @param cityCode     市编码
     * @param countyCode   区县编码
     * @param streetCode   乡镇街编码
     * @param villageCode  村编码
     */
    @GetMapping("/stat")
    public R<Map<String, Object>> statPopulationAndHouse(
        @RequestParam(value = "areaType", required = false) Integer areaType,
        @RequestParam(value = "provinceCode", required = false) String provinceCode,
        @RequestParam(value = "cityCode", required = false) String cityCode,
        @RequestParam(value = "countyCode", required = false) String countyCode,
        @RequestParam(value = "streetCode", required = false) String streetCode,
        @RequestParam(value = "villageCode", required = false) String villageCode) {
        return R.ok(dataPersonService.statPopulationAndHouse(
            areaType, provinceCode, cityCode, countyCode, streetCode, villageCode));
    }

    /**
     * 根据中心点查询 5 米范围内最近建筑的人口画像
     *
     * @param longitude 中心点经度
     * @param latitude  中心点纬度
     */
    @GetMapping("/building/profile")
    public R<BuildingPopulationProfileVo> getBuildingPopulationProfileByPoint(
        @RequestParam("longitude") @NotNull(message = "经度不能为空") BigDecimal longitude,
        @RequestParam("latitude") @NotNull(message = "纬度不能为空") BigDecimal latitude,
        @RequestParam(value = "radius", required = false) Integer radius) {
        return R.ok(dataPersonService.getBuildingPopulationProfileByPoint(longitude, latitude, radius));
    }
}
