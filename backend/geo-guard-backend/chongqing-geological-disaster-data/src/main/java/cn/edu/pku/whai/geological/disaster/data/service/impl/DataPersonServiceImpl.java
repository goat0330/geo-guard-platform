/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.dto.YbssSyrkDto;
import cn.edu.pku.whai.geological.disaster.data.domain.po.House;
import cn.edu.pku.whai.geological.disaster.data.domain.po.Person;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.BuildingPopulationProfileVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.BuildingPopulationStatsVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.NearestHouseCimVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.AdRegionMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataHouseMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataPersonMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.SlopeUnitMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IDataPersonService;
import cn.edu.pku.whai.geological.disaster.data.utils.JacksonUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 人员信息Service业务层处理
 *
 * @author system
 * @date 2024
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DataPersonServiceImpl implements IDataPersonService {

    private static final BigDecimal SQUARE_METERS_PER_SQUARE_KILOMETER = BigDecimal.valueOf(1_000_000L);
    private static final BigDecimal ZERO_RATIO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final int AREA_SCALE = 2;
    private static final int POINT_QUERY_RADIUS_METERS = 5;
    private static final BigDecimal PILOT_AREA_1_AREA_SQUARE_KILOMETER = new BigDecimal("561.46");
    private static final BigDecimal PILOT_AREA_2_AREA_SQUARE_KILOMETER = new BigDecimal("46.6");
    private static final String ENSHI_BUILDING_NAME_PREFIX = "湖北省恩施土家族苗族自治州恩施市";

    private final DataPersonMapper baseMapper;
    private final DataHouseMapper dataHouseMapper;
    private final AdRegionMapper adRegionMapper;
    private final SlopeUnitMapper slopeUnitMapper;

    /**
     * 批量保存人员数据
     *
     * @param dataList 人员数据列表
     */
    @Override
    public void saveBatch(List<YbssSyrkDto> dataList) {
        try {
            if (dataList == null || dataList.isEmpty()) {
                log.warn("人员数据列表为空，跳过保存");
                return;
            }

            List<Person> poList = dataList.stream()
                                          .map(dto -> MapstructUtils.convert(dto, Person.class))
                                          .toList();

            baseMapper.insertOrUpdateBatch(poList);
            log.info("成功保存 {} 条人员数据", poList.size());
        } catch (Exception e) {
            log.error("批量保存人员数据失败", e);
            throw new ServiceException("批量保存人员数据失败：" + e.getMessage());
        }
    }

    @Override
    public long countByScope(BigDecimal lonMax, BigDecimal latMax, BigDecimal lonMin, BigDecimal latMin) {
        LambdaQueryWrapper<Person> wrapper = buildScopeWrapper(lonMax, latMax, lonMin, latMin);
        return baseMapper.selectCount(wrapper);
    }

    @Override
    public List<String> listPhoneByScope(BigDecimal lonMax, BigDecimal latMax, BigDecimal lonMin, BigDecimal latMin) {
        LambdaQueryWrapper<Person> wrapper = buildScopeWrapper(lonMax, latMax, lonMin, latMin);
        return baseMapper.selectList(wrapper).stream()
                         .map(Person::getPhoneNumber)
                         .filter(Objects::nonNull)
                         .filter(phone -> !phone.isBlank())
                         .distinct()
                         .toList();
    }

    @Override
    public List<String> listPhoneByPolygonWkt(String polygonWkt) {
        if (StringUtils.isBlank(polygonWkt)) {
            return List.of();
        }
        List<String> polygonWkts = resolvePolygonWkts(polygonWkt);
        if (polygonWkts.isEmpty()) {
            return List.of();
        }
        QueryWrapper<Person> wrapper = new QueryWrapper<>();
        wrapper.isNotNull("longitude").isNotNull("latitude");
        wrapper.and(queryWrapper -> {
            boolean hasCondition = false;
            for (String areaWkt : polygonWkts) {
                if (StringUtils.isBlank(areaWkt)) {
                    continue;
                }
                if (!hasCondition) {
                    queryWrapper.apply(
                        "ST_Contains(ST_GeomFromText({0}, 4490), ST_SetSRID(ST_MakePoint(longitude::double precision, latitude::double precision), 4490))",
                        areaWkt
                    );
                    hasCondition = true;
                } else {
                    queryWrapper.or().apply(
                        "ST_Contains(ST_GeomFromText({0}, 4490), ST_SetSRID(ST_MakePoint(longitude::double precision, latitude::double precision), 4490))",
                        areaWkt
                    );
                }
            }
            if (!hasCondition) {
                queryWrapper.apply("1 = 0");
            }
        });
        return baseMapper.selectList(wrapper).stream()
                         .map(Person::getPhoneNumber)
                         .filter(Objects::nonNull)
                         .filter(phone -> !phone.isBlank())
                         .distinct()
                         .toList();
    }

    @Override
    public List<String> listPhoneByEvacuationArea(String evacuationArea) {
        if (StringUtils.isBlank(evacuationArea)) {
            return List.of();
        }
        List<String> candidateBuildingNames = buildCandidateBuildingNames(evacuationArea);
        if (candidateBuildingNames.isEmpty()) {
            return List.of();
        }
        List<House> houses = dataHouseMapper.selectList(
            Wrappers.<House>lambdaQuery()
                .in(House::getBuildingName, candidateBuildingNames)
                .isNotNull(House::getLongitude)
                .isNotNull(House::getLatitude)
        );
        if (houses == null || houses.isEmpty()) {
            log.warn("撤离区域未匹配到房屋，evacuationArea={}", evacuationArea);
            return List.of();
        }

        Set<BigDecimal> longitudeSet = new LinkedHashSet<>();
        Set<BigDecimal> latitudeSet = new LinkedHashSet<>();
        Set<String> coordinateKeys = new LinkedHashSet<>();
        for (House house : houses) {
            if (house == null || house.getLongitude() == null || house.getLatitude() == null) {
                continue;
            }
            longitudeSet.add(house.getLongitude());
            latitudeSet.add(house.getLatitude());
            coordinateKeys.add(buildCoordinateKey(house.getLongitude(), house.getLatitude()));
        }
        if (coordinateKeys.isEmpty()) {
            return List.of();
        }

        List<Person> persons = baseMapper.selectList(
            Wrappers.<Person>lambdaQuery()
                .in(Person::getLongitude, longitudeSet)
                .in(Person::getLatitude, latitudeSet)
        );
        if (persons == null || persons.isEmpty()) {
            return List.of();
        }
        return persons.stream()
            .filter(Objects::nonNull)
            .filter(person -> person.getLongitude() != null && person.getLatitude() != null)
            .filter(person -> coordinateKeys.contains(buildCoordinateKey(person.getLongitude(), person.getLatitude())))
            .map(Person::getPhoneNumber)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(phone -> !phone.isBlank())
            .distinct()
            .toList();
    }

    @Override
    public long countHouseholdByScope(BigDecimal lonMax, BigDecimal latMax, BigDecimal lonMin, BigDecimal latMin) {
        LambdaQueryWrapper<Person> wrapper = buildScopeWrapper(lonMax, latMax, lonMin, latMin);
        Long count = baseMapper.countDistinctHouseUnitId(wrapper);
        return count != null ? count : 0L;
    }

    @Override
    public long countByBuildingCode(String buildingCode) {
        if (StringUtils.isBlank(buildingCode)) {
            return 0L;
        }
        Long count = baseMapper.countByBuildingCode(buildingCode.trim());
        return count == null ? 0L : count;
    }

    @Override
    public long countByPolygonWkt(String polygonWkt) {
        if (StringUtils.isBlank(polygonWkt)) {
            return 0L;
        }
        Long count = baseMapper.countByPolygonWkt(polygonWkt.trim());
        return count == null ? 0L : count;
    }

    @Override
    public long countHouseholdByPolygonWkt(String polygonWkt) {
        if (StringUtils.isBlank(polygonWkt)) {
            return 0L;
        }
        Long count = baseMapper.countHouseholdByPolygonWkt(polygonWkt.trim());
        return count == null ? 0L : count;
    }

    @Override
    public Map<String, Object> statPopulationAndHouse(Integer areaType, String provinceCode, String cityCode, String countyCode,
                                                      String streetCode, String villageCode) {
        QueryWrapper<Person> personWrapper = buildAreaStatWrapper(areaType, provinceCode, cityCode, countyCode, streetCode,
            villageCode);
        QueryWrapper<House> houseWrapper = buildAreaStatWrapper(areaType, provinceCode, cityCode, countyCode, streetCode,
            villageCode);

        Map<String, Object> result = new LinkedHashMap<>(4);
        result.put("personCount", baseMapper.selectCount(personWrapper));
        result.put("houseCount", dataHouseMapper.selectCount(houseWrapper));
        result.put("area", resolveArea(areaType, provinceCode, cityCode, countyCode, streetCode, villageCode));
        result.put("slopeUnitCount", resolveSlopeUnitCount(areaType, provinceCode, cityCode, countyCode, streetCode, villageCode));
        return result;
    }

    @Override
    public BuildingPopulationProfileVo getBuildingPopulationProfileByPoint(BigDecimal longitude, BigDecimal latitude, Integer radius) {
        validateCoordinate(longitude, latitude);
        if (radius == null) {
            radius = POINT_QUERY_RADIUS_METERS;
        }
        NearestHouseCimVo nearestHouse = dataHouseMapper.selectNearestHouseByPoint(longitude, latitude, radius);
        if (nearestHouse == null || StringUtils.isBlank(nearestHouse.getBuildingCode())) {
            return null;
        }

        BuildingPopulationStatsVo stats = baseMapper.selectBuildingPopulationStats(nearestHouse.getBuildingCode());
        BuildingPopulationProfileVo result = new BuildingPopulationProfileVo();
        result.setBuildingName(stripEnshiBuildingNamePrefix(nearestHouse.getBuildingName()));
        result.setBuildingArea(defaultBigDecimal(nearestHouse.getArea()));

        long personCount = defaultLong(stats == null ? null : stats.getPersonCount());
        long maleCount = defaultLong(stats == null ? null : stats.getMaleCount());
        long femaleCount = defaultLong(stats == null ? null : stats.getFemaleCount());

        result.setPersonCount(personCount);
        result.setAge0To18Count(defaultLong(stats == null ? null : stats.getAge0To18Count()));
        result.setAge18To45Count(defaultLong(stats == null ? null : stats.getAge18To45Count()));
        result.setAge45To65Count(defaultLong(stats == null ? null : stats.getAge45To65Count()));
        result.setAge65AndAboveCount(defaultLong(stats == null ? null : stats.getAge65AndAboveCount()));
        result.setMaleRatio(calculateRatio(maleCount, personCount));
        result.setFemaleRatio(calculateRatio(femaleCount, personCount));
        result.setCarrierWkt(nearestHouse.getWkt());
        return result;
    }

    /**
     * 构建经纬度范围查询条件：经度在 [lonMin, lonMax]，纬度在 [latMin, latMax]
     */
    private LambdaQueryWrapper<Person> buildScopeWrapper(BigDecimal lonMax, BigDecimal latMax,
                                                         BigDecimal lonMin, BigDecimal latMin) {
        return Wrappers.<Person>lambdaQuery()
                       .between(lonMin != null && lonMax != null, Person::getLongitude, lonMin, lonMax)
                       .between(latMin != null && latMax != null, Person::getLatitude, latMin, latMax);
    }

    private <T> QueryWrapper<T> buildAreaStatWrapper(Integer areaType, String provinceCode, String cityCode, String countyCode,
                                                     String streetCode, String villageCode) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        applyAreaTypeFilter(wrapper, areaType);
        wrapper.eq(StringUtils.isNotBlank(provinceCode), "province_code", provinceCode);
        wrapper.eq(StringUtils.isNotBlank(cityCode), "city_code", cityCode);
        wrapper.eq(StringUtils.isNotBlank(countyCode), "county_code", countyCode);
        wrapper.eq(StringUtils.isNotBlank(streetCode), "street_code", streetCode);
        wrapper.eq(StringUtils.isNotBlank(villageCode), "village_code", villageCode);
        return wrapper;
    }

    private void applyAreaTypeFilter(QueryWrapper<?> wrapper, Integer areaType) {
        if (areaType == null) {
            return;
        }
        if (Integer.valueOf(1).equals(areaType)) {
            wrapper.eq("pilot_area_1", 1);
            return;
        }
        if (Integer.valueOf(2).equals(areaType)) {
            wrapper.eq("pilot_area_2", 1);
            return;
        }
        throw new ServiceException("areaType仅支持1（试点区）或2（示范区）");
    }

    private Double resolveArea(Integer areaType, String provinceCode, String cityCode, String countyCode,
                               String streetCode, String villageCode) {
        if (Integer.valueOf(1).equals(areaType)) {
            return toAreaDouble(PILOT_AREA_1_AREA_SQUARE_KILOMETER);
        }
        if (Integer.valueOf(2).equals(areaType)) {
            return toAreaDouble(PILOT_AREA_2_AREA_SQUARE_KILOMETER);
        }
        String regionCode = resolveRegionCode(provinceCode, cityCode, countyCode, streetCode, villageCode);
        if (StringUtils.isBlank(regionCode)) {
            return toAreaDouble(BigDecimal.ZERO);
        }
        BigDecimal area = adRegionMapper.selectAreaByRegionCode(regionCode);
        if (area == null) {
            return toAreaDouble(BigDecimal.ZERO);
        }
        return toAreaDouble(area.divide(SQUARE_METERS_PER_SQUARE_KILOMETER, AREA_SCALE, RoundingMode.HALF_UP));
    }

    private Long resolveSlopeUnitCount(Integer areaType, String provinceCode, String cityCode, String countyCode,
                                       String streetCode, String villageCode) {
        QueryWrapper<SlopeUnit> wrapper = Wrappers.query();
        wrapper.eq(areaType == 1, "pilot_area_1", 1);
        wrapper.eq(areaType == 2, "pilot_area_2", 1);
        wrapper.eq(StringUtils.isNotBlank(provinceCode), "province_code", provinceCode);
        wrapper.eq(StringUtils.isNotBlank(cityCode), "city_code", cityCode);
        wrapper.eq(StringUtils.isNotBlank(countyCode), "county_code", countyCode);
        wrapper.eq(StringUtils.isNotBlank(streetCode), "street_code", streetCode);
        wrapper.eq(StringUtils.isNotBlank(villageCode), "village_code", villageCode);
        return slopeUnitMapper.selectCount(wrapper);
    }

    private String resolveRegionCode(String provinceCode, String cityCode, String countyCode, String streetCode, String villageCode) {
        if (StringUtils.isNotBlank(villageCode)) {
            return villageCode;
        }
        if (StringUtils.isNotBlank(streetCode)) {
            return streetCode;
        }
        if (StringUtils.isNotBlank(countyCode)) {
            return countyCode;
        }
        if (StringUtils.isNotBlank(cityCode)) {
            return cityCode;
        }
        return provinceCode;
    }

    private Double toAreaDouble(BigDecimal area) {
        return area.setScale(AREA_SCALE, RoundingMode.HALF_UP).doubleValue();
    }

    private void validateCoordinate(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null) {
            throw new ServiceException("经纬度不能为空");
        }
        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new ServiceException("经度超出有效范围");
        }
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new ServiceException("纬度超出有效范围");
        }
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value;
    }

    private BigDecimal calculateRatio(long numerator, long denominator) {
        if (denominator <= 0) {
            return ZERO_RATIO;
        }
        return BigDecimal.valueOf(numerator)
                         .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    /**
     * 兼容单个WKT和JSON数组字符串两种格式。
     */
    private List<String> resolvePolygonWkts(String polygonWkt) {
        String text = polygonWkt == null ? null : polygonWkt.trim();
        if (StringUtils.isBlank(text)) {
            return List.of();
        }
        if (!text.startsWith("[")) {
            return List.of(text);
        }
        List<String> values = JacksonUtil.parseObject(text, new TypeReference<>() {
        });
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>(values.size());
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                result.add(value.trim());
            }
        }
        return result;
    }

    private List<String> buildCandidateBuildingNames(String evacuationArea) {
        String trimmed = evacuationArea == null ? null : evacuationArea.trim();
        if (StringUtils.isBlank(trimmed)) {
            return List.of();
        }
        Set<String> names = new LinkedHashSet<>();
        names.add(trimmed);
        String stripped = stripEnshiBuildingNamePrefix(trimmed);
        if (StringUtils.isNotBlank(stripped)) {
            names.add(stripped);
        }
        if (!trimmed.startsWith(ENSHI_BUILDING_NAME_PREFIX)) {
            names.add(ENSHI_BUILDING_NAME_PREFIX + trimmed);
        }
        if (StringUtils.isNotBlank(stripped) && !stripped.startsWith(ENSHI_BUILDING_NAME_PREFIX)) {
            names.add(ENSHI_BUILDING_NAME_PREFIX + stripped);
        }
        return new ArrayList<>(names);
    }

    private String buildCoordinateKey(BigDecimal longitude, BigDecimal latitude) {
        return normalizeCoordinate(longitude) + "|" + normalizeCoordinate(latitude);
    }

    private String normalizeCoordinate(BigDecimal coordinate) {
        if (coordinate == null) {
            return "";
        }
        return coordinate.stripTrailingZeros().toPlainString();
    }

    private String stripEnshiBuildingNamePrefix(String buildingName) {
        if (StringUtils.isBlank(buildingName)) {
            return buildingName;
        }
        String trimmed = buildingName.trim();
        if (trimmed.startsWith(ENSHI_BUILDING_NAME_PREFIX)) {
            return trimmed.substring(ENSHI_BUILDING_NAME_PREFIX.length()).trim();
        }
        return trimmed;
    }
}
