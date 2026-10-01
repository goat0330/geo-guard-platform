/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;


import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import cn.hutool.core.collection.CollUtil;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.MonitorPointBo;
import cn.edu.pku.whai.geological.disaster.data.domain.dto.MonitorPointDto;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorPoint;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorDeviceVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorPointVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorDeviceWarningLevelVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.MonitorDeviceMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.MonitorPointMapper;
import cn.edu.pku.whai.geological.disaster.data.props.DisasterPreventionPlatformProps;
import cn.edu.pku.whai.geological.disaster.data.service.IMonitorPointService;
import cn.edu.pku.whai.geological.disaster.data.utils.LevelCodeUtil;
import cn.edu.pku.whai.geological.disaster.data.utils.JacksonUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.kongweiguang.http.client.Req;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 监测点基本情况Service业务层处理
 *
 * @author lizheng
 * @date 2026-01-06
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class MonitorPointServiceImpl implements IMonitorPointService {

    private final MonitorPointMapper baseMapper;

    private final MonitorDeviceMapper monitorDeviceMapper;

    private final DisasterPreventionPlatformProps disasterPreventionPlatformProps;

    /**
     * 查询监测点基本情况
     *
     * @param id 主键
     * @return 监测点基本情况
     */
    @Override
    public MonitorPointVo queryById(String id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询监测点基本情况列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 监测点基本情况分页列表
     */
    @Override
    public TableDataInfo<MonitorPointVo> queryPageList(MonitorPointBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<MonitorPoint> lqw = buildQueryWrapper(bo);
        Page<MonitorPointVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<MonitorPointVo> queryByWkt(String wkt) {
        String normalizedWkt = validateGeometryWkt(wkt);
        QueryWrapper<MonitorPoint> queryWrapper = Wrappers.query();
        queryWrapper.isNotNull("longitude")
            .isNotNull("latitude")
            .and(wrapper -> wrapper.apply(
                "ST_Contains(ST_GeomFromText({0}, 4490), ST_SetSRID(ST_MakePoint(longitude::double precision, latitude::double precision), 4490))",
                normalizedWkt
            ));
        return baseMapper.selectVoList(queryWrapper);
    }

    @Override
    public List<MonitorPointVo> queryByMonitorNames(List<String> monitorPointNames) {
        if (CollUtil.isEmpty(monitorPointNames)) {
            return List.of();
        }
        List<String> normalizedNames = monitorPointNames.stream()
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .distinct()
            .collect(Collectors.toList());
        if (CollUtil.isEmpty(normalizedNames)) {
            return List.of();
        }
        List<MonitorPointVo> monitorPoints = baseMapper.selectVoListByMonitorNames(normalizedNames);
        if (CollUtil.isEmpty(monitorPoints)) {
            return List.of();
        }

        List<String> monitoringPointIds = monitorPoints.stream()
            .map(MonitorPointVo::getId)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .collect(Collectors.toList());
        Map<String, List<MonitorDeviceVo>> devicesByPointId = CollUtil.isEmpty(monitoringPointIds)
            ? Map.of()
            : monitorDeviceMapper.selectVoListByMonitoringPointIds(monitoringPointIds).stream()
                .collect(Collectors.groupingBy(MonitorDeviceVo::getMonitoringPointId, LinkedHashMap::new, Collectors.toList()));

        Map<String, Integer> warningLevelByDeviceId = buildWarningLevelByDeviceId(devicesByPointId);
        monitorPoints.forEach(monitorPoint -> {
            List<MonitorDeviceVo> deviceList = devicesByPointId.getOrDefault(monitorPoint.getId(), List.of());
            deviceList.forEach(device -> device.setWarningLevel(warningLevelByDeviceId.get(device.getId())));
            monitorPoint.setMonitorDeviceList(deviceList);
        });

        Map<String, List<MonitorPointVo>> pointsByName = monitorPoints.stream()
            .collect(Collectors.groupingBy(MonitorPointVo::getMonitorName, LinkedHashMap::new, Collectors.toList()));

        List<MonitorPointVo> orderedResult = new ArrayList<>();
        for (String monitorPointName : normalizedNames) {
            List<MonitorPointVo> points = pointsByName.get(monitorPointName);
            if (CollUtil.isNotEmpty(points)) {
                orderedResult.addAll(points);
            }
        }
        return orderedResult;
    }

    @Override
    public void syncData() {
        try {
            List<MonitorPointDto> monitorPointDtoList = new ArrayList<>();

            String response = Req
                    .post(disasterPreventionPlatformProps.getUrl())
                    .path("/hbdz-extapi/jcdgl/pageJcdgl")
                    .header("token", disasterPreventionPlatformProps.getToken())
                    .json(JacksonUtil.objectMapper.createObjectNode().toString())
                    .timeout(Duration.ofMinutes(3)).ok().str();

            JsonNode root = JacksonUtil.objectMapper.readTree(response);
            JsonNode dataNode = root.get("data").get("list");

            if (dataNode != null && dataNode.isArray()) {
                monitorPointDtoList = JacksonUtil.objectMapper.convertValue(
                        dataNode,
                        JacksonUtil.objectMapper.getTypeFactory().constructCollectionType(List.class, MonitorPointDto.class)
                );
            }

            List<MonitorPoint> monitorPointList = new ArrayList<>();
            monitorPointDtoList.forEach(dto -> {
                monitorPointList.add(MapstructUtils.convert(dto, MonitorPoint.class));
            });

            baseMapper.insertOrUpdateBatch(monitorPointList);
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    private LambdaQueryWrapper<MonitorPoint> buildQueryWrapper(MonitorPointBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<MonitorPoint> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getPilotArea1() != null, MonitorPoint::getPilotArea1, bo.getPilotArea1());
        lqw.eq(bo.getPilotArea2() != null, MonitorPoint::getPilotArea2, bo.getPilotArea2());
        lqw.eq(StringUtils.isNotBlank(bo.getProvinceCode()), MonitorPoint::getProvinceCode, bo.getProvinceCode());
        lqw.eq(StringUtils.isNotBlank(bo.getCityCode()), MonitorPoint::getCityCode, bo.getCityCode());
        lqw.eq(StringUtils.isNotBlank(bo.getCountyCode()), MonitorPoint::getCountyCode, bo.getCountyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getStreetCode()), MonitorPoint::getStreetCode, bo.getStreetCode());
        lqw.eq(StringUtils.isNotBlank(bo.getVillageCode()), MonitorPoint::getVillageCode, bo.getVillageCode());
        lqw.eq(StringUtils.isNotBlank(bo.getProvince()), MonitorPoint::getProvince, bo.getProvince());
        lqw.eq(StringUtils.isNotBlank(bo.getCity()), MonitorPoint::getCity, bo.getCity());
        lqw.eq(StringUtils.isNotBlank(bo.getCounty()), MonitorPoint::getCounty, bo.getCounty());
        lqw.eq(StringUtils.isNotBlank(bo.getStreet()), MonitorPoint::getStreet, bo.getStreet());
        lqw.eq(StringUtils.isNotBlank(bo.getVillage()), MonitorPoint::getVillage, bo.getVillage());
        lqw.orderByAsc(MonitorPoint::getId);
        return lqw;
    }


    @Override
    public List<MonitorPointVo> queryBySlopeUnitId(String id) {
        LambdaQueryWrapper<MonitorPoint> lqw = Wrappers.lambdaQuery();
        lqw.eq(MonitorPoint::getSlopeUnitId, id);
        return baseMapper.selectVoList(lqw);
    }

    private Map<String, Integer> buildWarningLevelByDeviceId(Map<String, List<MonitorDeviceVo>> devicesByPointId) {
        List<String> deviceIds = devicesByPointId.values().stream()
            .flatMap(List::stream)
            .map(MonitorDeviceVo::getId)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .collect(Collectors.toList());
        if (CollUtil.isEmpty(deviceIds)) {
            return Map.of();
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        monitorDeviceMapper.selectLatestWarningLevelByDeviceIds(deviceIds).forEach(item -> {
            if (StringUtils.isBlank(item.getDeviceId())) {
                return;
            }
            Integer warningLevel = parseWarningLevel(item.getWarningLevel());
            if (warningLevel != null) {
                result.putIfAbsent(item.getDeviceId(), warningLevel);
            }
        });
        return result;
    }

    private Integer parseWarningLevel(String rawWarningLevel) {
        if (StringUtils.isBlank(rawWarningLevel)) {
            return null;
        }
        try {
            return Integer.parseInt(rawWarningLevel.trim());
        } catch (Exception ignore) {
            return LevelCodeUtil.convertThirdPartyCodeToNormalizedWarningLevel(rawWarningLevel);
        }
    }

    private String validateGeometryWkt(String wkt) {
        if (StringUtils.isBlank(wkt)) {
            throw new ServiceException("wkt不能为空");
        }
        String normalized = wkt.trim();
        try {
            Geometry geometry = new WKTReader().read(normalized);
            if (geometry == null || geometry.isEmpty() || geometry.getDimension() < 2) {
                throw new ServiceException("wkt格式错误");
            }
            return normalized;
        } catch (ParseException e) {
            throw new ServiceException("wkt格式错误");
        }
    }
}
