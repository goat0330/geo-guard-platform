/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;


import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.MonitorDeviceBo;
import cn.edu.pku.whai.geological.disaster.data.domain.dto.MonitorDeviceDto;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorDevice;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorPoint;
import cn.edu.pku.whai.geological.disaster.data.domain.req.MonitorStatReq;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.MonitorDeviceObjectResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.MonitorDeviceTreeResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.MonitorSensorObjectResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorDeviceStatByHazardVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorDeviceVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorStatVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.MonitorDeviceMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.MonitorPointMapper;
import cn.edu.pku.whai.geological.disaster.data.props.DisasterPreventionPlatformProps;
import cn.edu.pku.whai.geological.disaster.data.service.IMonitorDeviceService;
import cn.edu.pku.whai.geological.disaster.data.utils.JacksonUtil;
import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.kongweiguang.http.client.Req;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 监测设备基本情况Service业务层处理
 *
 * @author lizheng
 * @date 2026-01-06
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class MonitorDeviceServiceImpl implements IMonitorDeviceService {

    /**
     * 设备类型：A - 设备
     */
    private static final String DEVICE_TYPE_A = "A";

    /**
     * 设备类型：B - 传感器
     */
    private static final String DEVICE_TYPE_B = "B";

    private final MonitorDeviceMapper baseMapper;

    private final MonitorPointMapper monitorPointMapper;

    private final DisasterPreventionPlatformProps disasterPreventionPlatformProps;

    /**
     * 查询监测设备基本情况
     *
     * @param id 主键
     * @return 监测设备基本情况
     */
    @Override
    public MonitorDeviceVo queryById(String id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询监测设备基本情况列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 监测设备基本情况分页列表
     */
    @Override
    public TableDataInfo<MonitorDeviceVo> queryPageList(MonitorDeviceBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<MonitorDevice> lqw = buildQueryWrapper(bo);
        Page<MonitorDeviceVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public void syncData() {
        AtomicInteger count = new AtomicInteger(0);
        try {
            List<MonitorPoint> monitorPoints = monitorPointMapper.selectList(new QueryWrapper<MonitorPoint>().orderByAsc("monitor_code"));
            monitorPoints.forEach(dataMonitorPoint -> {
                List<MonitorDeviceDto> monitorDeviceDtoList = new ArrayList<>();
                log.debug("同步监测点数据，监测点编号: {}", dataMonitorPoint.getMonitorCode());

                String response = Req.get(disasterPreventionPlatformProps.getUrl())
                        .path("/hbdz-extapi/sb/getSbListByJcdId")
                        .header("token", disasterPreventionPlatformProps.getToken())
                        .query("jcdId", dataMonitorPoint.getMonitorCode())
                        .timeout(Duration.ofMinutes(3))
                        .ok()
                        .str();

                JsonNode root;
                try {
                    root = JacksonUtil.objectMapper.readTree(response);

                    JsonNode dataNode = root.get("data");

                    if (dataNode != null && dataNode.isArray()) {
                        monitorDeviceDtoList = JacksonUtil.objectMapper.convertValue(
                                dataNode,
                                JacksonUtil.objectMapper.getTypeFactory().constructCollectionType(List.class, MonitorDeviceDto.class)
                        );
                    }

                    List<MonitorDevice> monitorDeviceList = new ArrayList<>();
                    monitorDeviceDtoList.forEach(dto -> {
                        monitorDeviceList.add(MapstructUtils.convert(dto, MonitorDevice.class));
                    });

                    baseMapper.insertOrUpdateBatch(monitorDeviceList);
                    count.incrementAndGet();
                    if (count.intValue() % 100 == 0) {
                        TimeUnit.SECONDS.sleep(2);
                    }
                } catch (JsonProcessingException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 获取监测设备监测数据
     *
     * @param id 主键
     */
    @Override
    public MonitorDeviceTreeResp queryMonitorDataById(String id) {
        try {
            String response = Req
                    .post(disasterPreventionPlatformProps.getUrl())
                    .path("/hbdz-extapi/sbck/getSbcgqTree")
                    .header("token", disasterPreventionPlatformProps.getToken())
                    .query("sbid", id)
                    .json(JacksonUtil.objectMapper.createObjectNode().toString())
                    .timeout(Duration.ofMinutes(3))
                    .ok()
                    .str();

            JsonNode root = JacksonUtil.objectMapper.readTree(response);
            JsonNode dataNode = root.get("data");

            if (dataNode == null) {
                return null;
            }

            // 将JsonNode转换为MonitorDeviceTreeResp对象
            MonitorDeviceTreeResp treeResp = JacksonUtil.objectMapper.convertValue(dataNode, MonitorDeviceTreeResp.class);

            // 递归处理树形结构，将object字段转换为正确的类型
            convertObjectTypes(treeResp);

            return treeResp;
        } catch (JsonProcessingException e) {
            log.error("解析监测设备数据失败，设备ID: {}", id, e);
            throw new RuntimeException("解析监测设备数据失败", e);
        }
    }

    private LambdaQueryWrapper<MonitorDevice> buildQueryWrapper(MonitorDeviceBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<MonitorDevice> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getPilotArea1() != null, MonitorDevice::getPilotArea1, bo.getPilotArea1());
        lqw.eq(bo.getPilotArea2() != null, MonitorDevice::getPilotArea2, bo.getPilotArea2());
        lqw.eq(StringUtils.isNotBlank(bo.getProvinceCode()), MonitorDevice::getProvinceCode, bo.getProvinceCode());
        lqw.eq(StringUtils.isNotBlank(bo.getCityCode()), MonitorDevice::getCityCode, bo.getCityCode());
        lqw.eq(StringUtils.isNotBlank(bo.getCountyCode()), MonitorDevice::getCountyCode, bo.getCountyCode());
        lqw.eq(StringUtils.isNotBlank(bo.getStreetCode()), MonitorDevice::getStreetCode, bo.getStreetCode());
        lqw.eq(StringUtils.isNotBlank(bo.getVillageCode()), MonitorDevice::getVillageCode, bo.getVillageCode());
        lqw.eq(StringUtils.isNotBlank(bo.getProvince()), MonitorDevice::getProvince, bo.getProvince());
        lqw.eq(StringUtils.isNotBlank(bo.getCity()), MonitorDevice::getCity, bo.getCity());
        lqw.eq(StringUtils.isNotBlank(bo.getCounty()), MonitorDevice::getCounty, bo.getCounty());
        lqw.eq(StringUtils.isNotBlank(bo.getStreet()), MonitorDevice::getStreet, bo.getStreet());
        lqw.eq(StringUtils.isNotBlank(bo.getVillage()), MonitorDevice::getVillage, bo.getVillage());
        lqw.orderByAsc(MonitorDevice::getId);
        return lqw;
    }


    @Override
    public MonitorStatVo stat() {
        MonitorStatVo vo = new MonitorStatVo();
        CompletableFuture<Void> t1 = CompletableFuture.runAsync(() -> {
            MonitorStatVo st = baseMapper.statMonitor();
            vo.setMonitorDeviceOnlineCount(st.getMonitorDeviceOnlineCount());
            vo.setMonitorDeviceCount(st.getMonitorDeviceCount());
            if (st.getMonitorDeviceCount() > 0) {
                vo.setOnlineRate(NumberUtil.div(st.getMonitorDeviceOnlineCount(), st.getMonitorDeviceCount()));
            }
        });

        CompletableFuture<Void> t2 = CompletableFuture.runAsync(() -> {
            LambdaQueryWrapper<MonitorPoint> lqw = Wrappers.lambdaQuery();
            lqw.eq(MonitorPoint::getPilotArea1, 1);
            Long count = monitorPointMapper.selectCount(lqw);
            vo.setMonitorPointCount(count);
        });

        CompletableFuture.allOf(t1, t2).join();

        return vo;
    }

    @Override
    public MonitorStatVo statByArea(MonitorStatReq req) {
        MonitorStatVo vo = new MonitorStatVo();
        CompletableFuture<Void> t1 = CompletableFuture.runAsync(() -> {
            MonitorStatVo st = baseMapper.statMonitorByArea(req);
            long monitorDeviceCount = st == null || st.getMonitorDeviceCount() == null ? 0L : st.getMonitorDeviceCount();
            long monitorDeviceOnlineCount = st == null || st.getMonitorDeviceOnlineCount() == null ? 0L : st.getMonitorDeviceOnlineCount();
            vo.setMonitorDeviceCount(monitorDeviceCount);
            vo.setMonitorDeviceOnlineCount(monitorDeviceOnlineCount);
            if (monitorDeviceCount > 0) {
                vo.setOnlineRate(NumberUtil.div(BigDecimal.valueOf(monitorDeviceOnlineCount), BigDecimal.valueOf(monitorDeviceCount)));
            } else {
                vo.setOnlineRate(BigDecimal.ZERO);
            }
        });

        CompletableFuture<Void> t2 = CompletableFuture.runAsync(() -> {
            Long count = monitorPointMapper.statCountByArea(req);
            vo.setMonitorPointCount(count == null ? 0L : count);
        });

        CompletableFuture.allOf(t1, t2).join();
        return vo;
    }

    /**
     * 递归转换树形结构中object字段的类型
     *
     * @param node 树节点
     */
    private void convertObjectTypes(MonitorDeviceTreeResp node) {
        if (node == null) {
            return;
        }

        // 转换当前节点的object字段
        if (node.getObject() != null) {
            Object obj = node.getObject();
            // 如果object是Map类型（Jackson反序列化后的默认类型），需要转换为具体类型
            if (obj instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;

                // 判断是否为传感器：优先检查cgqbh字段（传感器特有字段）
                if (map.containsKey("cgqbh")) {
                    // 传感器对象
                    node.setObject(JacksonUtil.objectMapper.convertValue(map, MonitorSensorObjectResp.class));
                } else {
                    // 获取deviceType或sbtype字段值来判断类型
                    // deviceType="A" 表示设备，deviceType="B" 表示传感器
                    Object deviceTypeObj = map.get("deviceType");
                    if (deviceTypeObj == null) {
                        deviceTypeObj = map.get("sbtype");
                    }

                    String deviceType = deviceTypeObj != null ? deviceTypeObj.toString() : null;

                    // 如果deviceType为"B"，则识别为传感器
                    if (DEVICE_TYPE_B.equals(deviceType)) {
                        // 传感器对象
                        node.setObject(JacksonUtil.objectMapper.convertValue(map, MonitorSensorObjectResp.class));
                    } else {
                        // 默认识别为设备对象（deviceType="A"或不存在时）
                        node.setObject(JacksonUtil.objectMapper.convertValue(map, MonitorDeviceObjectResp.class));
                    }
                }
            }
        }

        // 递归处理子节点
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            for (MonitorDeviceTreeResp child : node.getChildren()) {
                convertObjectTypes(child);
            }
        }
    }

    @Override
    public List<MonitorDeviceVo> queryBySlopeUnitId(String id) {
        LambdaQueryWrapper<MonitorDevice> lqw = Wrappers.lambdaQuery(MonitorDevice.class);
        lqw.eq(MonitorDevice::getSlopeUnitId, id);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 按灾害点唯一编号批量聚合统计设备台套/分类。
     * 入参为空时直接返回空集合，避免无效 SQL 命中。
     *
     * @param disasterIds 灾害点唯一编号列表
     * @return 统计结果列表
     */
    @Override
    public List<MonitorDeviceStatByHazardVo> statByDisasterIds(List<String> disasterIds) {
        if (disasterIds == null || disasterIds.isEmpty()) {
            return List.of();
        }
        return baseMapper.statByDisasterIds(disasterIds);
    }
}
