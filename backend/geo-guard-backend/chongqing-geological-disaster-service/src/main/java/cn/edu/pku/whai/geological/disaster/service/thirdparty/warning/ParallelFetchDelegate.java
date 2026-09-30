/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RiskWarningRecordBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyMonitorDeviceBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyWarningDisposalBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorDevice;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorPoint;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.MonitorWarningRecordPageResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.MonitorWarningRecordResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.ThirdPartyWarningDataResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorDeviceRuntimeVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyMonitorDeviceVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyWarningDisposalVo;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.DeviceLatestWarningInfo;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.JcdWarningFetchResult;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalStatusLookup;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalWarningLookup;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.MonitorPointWarnings;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.MonitorWarningDataResult;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.ThirdMonitorDeviceWarningPageResp;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.ThirdMonitorDeviceWarningResp;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.ThirdMonitorPointResp;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.ThirdMonitorWarningHandleDetailResp;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.WarningContext;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.kongweiguang.http.client.Req;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public class ParallelFetchDelegate {
    private final ThirdPartyWarningDataSplitSupport thirdPartyWarningDataSplitSupport;

    public ParallelFetchDelegate(ThirdPartyWarningDataSplitSupport thirdPartyWarningDataSplitSupport) {
        this.thirdPartyWarningDataSplitSupport = thirdPartyWarningDataSplitSupport;
    }

    /**
     * 单区划全量拉取 jcsbxx，失败时返回空列表
     */
    List<ThirdPartyMonitorDeviceVo> fetchAllMonitorDevicesForRegionSafe(String regionCode) {
        if (StrUtil.isBlank(regionCode)) {
            return new ArrayList<ThirdPartyMonitorDeviceVo>();
        }
        try {
            ThirdPartyMonitorDeviceBo bo = new ThirdPartyMonitorDeviceBo();
            bo.setRegionCode(regionCode);
            return fetchAllMonitorDevices(bo, true);
        } catch (Exception e) {
            ThirdPartyWarningDataSplitSupport.log.warn("分行政区划拉取监测设备失败, regionCode={}", regionCode, e);
            return new ArrayList<ThirdPartyMonitorDeviceVo>();
        }
    }

    /**
     * 按设备 sbid 取 yjsj 最新一条预警的 yjdj，并转为 1-4
     */
    Map<String, DeviceLatestWarningInfo> mergeLatestWarningBySbid(List<ThirdMonitorDeviceWarningResp> allWarnings) {
        if (allWarnings == null || allWarnings.isEmpty()) {
            return Map.of();
        }
        Map<String, String> localDeviceIdByPointAndName = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().loadMonitorDeviceIdByPointAndName(allWarnings);
        Map<String, LocalDateTime> bestTime = new HashMap<String, LocalDateTime>();
        Map<String, ThirdMonitorDeviceWarningResp> latestWarningMap = new HashMap<String, ThirdMonitorDeviceWarningResp>();
        for (ThirdMonitorDeviceWarningResp w : allWarnings) {
            String deviceId = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().resolveWarningDeviceId(w, localDeviceIdByPointAndName);
            if (w == null || StrUtil.isBlank(deviceId)) {
                continue;
            }
            LocalDateTime t = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().parseDateTime(w.getYjsj());
            if (t == null) {
                continue;
            }
            LocalDateTime old = bestTime.get(deviceId);
            if (old == null || t.isAfter(old)) {
                bestTime.put(deviceId, t);
                latestWarningMap.put(deviceId, w);
            }
        }
        Map<String, DeviceLatestWarningInfo> result = new HashMap<String, DeviceLatestWarningInfo>();
        for (Map.Entry<String, ThirdMonitorDeviceWarningResp> entry : latestWarningMap.entrySet()) {
            ThirdMonitorDeviceWarningResp warningResp = entry.getValue();
            result.put(
                    entry.getKey(),
                    new DeviceLatestWarningInfo(
                            thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().convertFromThirdPartyWarningLevel(warningResp.getYjdj()),
                            warningResp.getYjsj()
                    )
            );
        }
        return result;
    }

    /**
     * 组装脱敏后的运行时 VO
     */
    MonitorDeviceRuntimeVo toMonitorDeviceRuntimeVo(
            MonitorDevice d,
            DeviceLatestWarningInfo latestWarningInfo,
            Map<String, ThirdPartyMonitorDeviceVo> byId,
            Map<String, ThirdPartyMonitorDeviceVo> byClientId) {
        MonitorDeviceRuntimeVo vo = new MonitorDeviceRuntimeVo();
        BeanUtil.copyProperties(d, vo);
        vo.setWarningLevel(latestWarningInfo == null ? null : latestWarningInfo.warningLevel());
        vo.setWarningTime(latestWarningInfo == null ? null : latestWarningInfo.warningTime());
        ThirdPartyMonitorDeviceVo tp = byId.get(d.getId());
        if (tp == null) {
            tp = byClientId.get(d.getClientId());
        }
        String deviceRuntimeStatusName = tp == null ? null : tp.getDeviceStatusName();
        vo.setOnlineStatus(thirdPartyWarningDataSplitSupport.resolveOnlineStatus(deviceRuntimeStatusName));
        return vo;
    }

    /**
     * 分页拉取指定监测点下的全部设备
     */
    List<ThirdPartyMonitorDeviceVo> fetchAllMonitorDevices(ThirdPartyMonitorDeviceBo bo, boolean remote) {
        List<ThirdPartyMonitorDeviceVo> result = new ArrayList<ThirdPartyMonitorDeviceVo>();
        int pageNum = 1;
        while (true) {
            PageQuery pageQuery = new PageQuery(ThirdPartyWarningDataSplitSupport.THIRD_PARTY_PAGE_SIZE, pageNum);
            TableDataInfo<ThirdPartyMonitorDeviceVo> pageData = remote
                    ? thirdPartyWarningDataSplitSupport.getDeviceDataDelegate().getMonitorDevicePageFromRemote(bo, pageQuery)
                    : thirdPartyWarningDataSplitSupport.getDeviceDataDelegate().getMonitorDevicePage(bo, pageQuery);
            if (pageData == null || pageData.getData() == null || pageData.getData().isEmpty()) {
                break;
            }
            result.addAll(pageData.getData());
            if (pageData.getData().size() < ThirdPartyWarningDataSplitSupport.THIRD_PARTY_PAGE_SIZE || result.size() >= pageData.getTotal()) {
                break;
            }
            pageNum++;
        }
        return result;
    }

    /**
     * 调用监测预警接口获取全部数据
     *
     * @param bo 查询条件
     * @return 监测预警数据结果（包含列表和总数）
     */
    MonitorWarningDataResult fetchMonitorWarningData(RiskWarningRecordBo bo) {
        // 构建请求参数（只传开始时间和结束时间）
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("startTime", bo.getStartTime());
        params.put("endTime", bo.getEndTime());

        try {
            ThirdPartyWarningDataResp resp = Req.post(thirdPartyWarningDataSplitSupport.getDisasterPreventionPlatformProps().getUrl())
                                                .path("/hbdz-extapi/yjfx/yjcz/pageWarningInfo")
                                                .header("token", thirdPartyWarningDataSplitSupport.getDisasterPreventionPlatformProps().getToken())
                                                .json(params)
                                                .timeout(Duration.ofMinutes(3))
                                                .ok()
                                                .obj(ThirdPartyWarningDataResp.class);

            if (ObjectUtil.isNull(resp)) {
                ThirdPartyWarningDataSplitSupport.log.warn("获取监测预警数据失败: 响应为空");
                return new MonitorWarningDataResult(new ArrayList<MonitorWarningRecordResp>(), 0);
            }

            Object data = resp.getData();
            if (ObjectUtil.isNull(data)) {
                ThirdPartyWarningDataSplitSupport.log.warn("获取监测预警数据失败: 响应数据为空");
                return new MonitorWarningDataResult(new ArrayList<MonitorWarningRecordResp>(), 0);
            }

            // 解析为JSONObject（分页格式）
            JSONObject dataObj = JSONUtil.parseObj(data);
            MonitorWarningRecordPageResp pageResp = JSONUtil.toBean(dataObj, MonitorWarningRecordPageResp.class);
            if (ObjectUtil.isNotNull(pageResp)) {
                List<MonitorWarningRecordResp> records = ObjectUtil.isNotNull(pageResp.getList())
                        ? pageResp.getList()
                        : new ArrayList<MonitorWarningRecordResp>();
                records = records.stream()
                                 .filter(record -> record != null && StrUtil.contains(record.getAddress(), "恩施市"))
                                 .collect(Collectors.toList());
                Integer total = records.size();
                return new MonitorWarningDataResult(records, total);
            }

            return new MonitorWarningDataResult(new ArrayList<MonitorWarningRecordResp>(), 0);
        } catch (Exception e) {
            ThirdPartyWarningDataSplitSupport.log.error("获取监测预警数据失败: {}", e.getMessage());
            throw new ServiceException("获取监测预警数据失败: " + e.getMessage());
        }
    }

    /**
     * 从本地库查询监测预警记录（用于统计概览），时间条件与落库表 {@code data_tp_warning_event} 一致，
     * 区域条件与三方接口返回后的「地址含恩施」过滤对齐。
     */
    MonitorWarningDataResult fetchMonitorWarningDataFromLocal(RiskWarningRecordBo bo) {
        thirdPartyWarningDataSplitSupport.getWarningQueryGuardDelegate().validateParams(bo);
        Timestamp startTime;
        Timestamp endTime;
        try {
            startTime = Timestamp.valueOf(LocalDateTime.parse(bo.getStartTime(), ThirdPartyWarningDataSplitSupport.DATE_TIME_FORMATTER));
            endTime = Timestamp.valueOf(LocalDateTime.parse(bo.getEndTime(), ThirdPartyWarningDataSplitSupport.DATE_TIME_FORMATTER));
        } catch (Exception e) {
            ThirdPartyWarningDataSplitSupport.log.warn("解析监测预警统计时间范围失败: {} ~ {}", bo.getStartTime(), bo.getEndTime(), e);
            return new MonitorWarningDataResult(new ArrayList<MonitorWarningRecordResp>(), 0);
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("startTime", startTime);
        params.addValue("endTime", endTime);
        params.addValue("enshiLike", "%恩施%");

        // noinspection SqlResolve
        String sql = """
                SELECT e.warning_level, e.disposal_status, e.disposal_time, e.disposal_type, e.disposal_person
                FROM data_tp_warning_event e
                LEFT JOIN v_monitor_point mp ON e.monitor_point_id = mp.id
                WHERE e.warning_time >= :startTime
                  AND e.warning_time <= :endTime
                """;

        List<MonitorWarningRecordResp> records = thirdPartyWarningDataSplitSupport.getNamedParameterJdbcTemplate().query(sql, params, (rs, rowNum) -> {
            MonitorWarningRecordResp r = new MonitorWarningRecordResp();
            r.setWarningLevel(thirdPartyWarningDataSplitSupport.resolveMonitorStatWarningLevel(rs.getString("warning_level")));
            r.setWarningProcessStatus(thirdPartyWarningDataSplitSupport.mapMonitorWarningProcessStatusLabel(
                    thirdPartyWarningDataSplitSupport.getNullableInt(rs, "disposal_status"),
                    rs.getTimestamp("disposal_time"),
                    rs.getString("disposal_type"),
                    rs.getString("disposal_person")
            ));
            return r;
        });

        return new MonitorWarningDataResult(records, records.size());
    }

    /**
     * 拉取并组装预警处置列表数据
     *
     * @param bo                查询条件
     * @param targetRecordCount 目标记录数，用于无详情筛选时提前结束查询
     * @return 预警处置列表
     */
    List<ThirdPartyWarningDisposalVo> fetchAllMonitorWarningDisposal(ThirdPartyWarningDisposalBo bo, int targetRecordCount) {
        boolean needDetailFilter = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().needDetailFilter(bo);
        boolean canEarlyStop = !needDetailFilter && targetRecordCount > 0;
        List<ThirdPartyWarningDisposalVo> result = new ArrayList<ThirdPartyWarningDisposalVo>();
        String fallbackRegionName = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().resolveRegionNameByCode(bo.getRegionCode());
        List<MonitorPoint> localMonitorPoints = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().queryLocalMonitorPoints(bo);
        if (localMonitorPoints.isEmpty()) {
            return result;
        }

        Map<String, MonitorPoint> localMonitorPointMap = thirdPartyWarningDataSplitSupport.getLocalBindingDelegate().buildLocalMonitorPointMap(localMonitorPoints);
        List<ThirdMonitorPointResp> monitorPoints = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().convertToThirdMonitorPoints(localMonitorPoints);
        List<MonitorPointWarnings> monitorPointWarningsList = fetchMonitorPointWarningsParallel(monitorPoints, bo);
        List<WarningContext> warningContexts = new ArrayList<WarningContext>();
        for (MonitorPointWarnings monitorPointWarnings : monitorPointWarningsList) {
            for (ThirdMonitorDeviceWarningResp warningResp : monitorPointWarnings.warningRecords()) {
                if (!thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().matchDateRange(warningResp.getYjsj(), bo.getWarningPublishTime())) {
                    continue;
                }
                warningContexts.add(new WarningContext(
                        monitorPointWarnings.monitorPointResp(),
                        warningResp,
                        thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().resolveLocalMonitorPoint(localMonitorPointMap, monitorPointWarnings.monitorPointResp())
                ));
            }
        }
        Map<String, ThirdMonitorWarningHandleDetailResp> detailRespMap = Map.of();
        if (needDetailFilter) {
            detailRespMap = fetchMonitorWarningHandleDetailParallel(warningContexts);
        }
        Set<String> pointIds = localMonitorPoints.stream()
                                                 .map(MonitorPoint::getId)
                                                 .filter(StrUtil::isNotBlank)
                                                 .collect(Collectors.toSet());
        Map<String, MonitorDevice> deviceById = thirdPartyWarningDataSplitSupport.getLocalBindingDelegate().loadMonitorDeviceById(pointIds);
        List<MonitorDevice> devices = new ArrayList<MonitorDevice>(deviceById.values());
        Map<String, String> localDeviceIdByPointAndName = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().buildMonitorDeviceIdByPointAndName(devices);
        Map<String, List<MonitorDevice>> devicesByPointId = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().buildMonitorDevicesByPointId(devices);
        LocalStatusLookup statusLookup = thirdPartyWarningDataSplitSupport.getLocalBindingDelegate().loadLocalStatusLookup(devices);
        LocalWarningLookup warningLookup = thirdPartyWarningDataSplitSupport.getLocalBindingDelegate().loadLocalWarningLookup(devices);
        for (WarningContext warningContext : warningContexts) {
            ThirdMonitorWarningHandleDetailResp detailResp = detailRespMap.get(warningContext.warningId());
            MonitorDevice device = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().resolveWarningDevice(
                    warningContext.warningResp(),
                    warningContext.localMonitorPoint(),
                    deviceById,
                    localDeviceIdByPointAndName,
                    devicesByPointId
            );
            ThirdPartyWarningDisposalVo vo = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().buildMonitorWarningDisposalVo(
                    warningContext.monitorPointResp(),
                    warningContext.warningResp(),
                    detailResp,
                    warningContext.localMonitorPoint(),
                    fallbackRegionName
            );
            thirdPartyWarningDataSplitSupport.getLocalBindingDelegate().enrichMonitorWarningDisposalRuntimeFields(
                    vo,
                    device,
                    thirdPartyWarningDataSplitSupport.getLocalBindingDelegate().resolveLocalStatus(device, statusLookup),
                    thirdPartyWarningDataSplitSupport.getLocalBindingDelegate().resolveLatestWarningInfo(device, warningLookup),
                    warningContext.warningResp().getYjsj()
            );
            if (!needDetailFilter || thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().matchMonitorWarningDisposal(bo, detailResp, vo)) {
                result.add(vo);
            }
            if (canEarlyStop && result.size() >= targetRecordCount) {
                break;
            }
        }
        return result;
    }

    /**
     * 并发按预警id查询处置详情
     */
    Map<String, ThirdMonitorWarningHandleDetailResp> fetchMonitorWarningHandleDetailByIdsParallel(List<String> warningIds) {
        if (warningIds == null || warningIds.isEmpty()) {
            return Map.of();
        }
        Map<String, ThirdMonitorWarningHandleDetailResp> detailMap = new ConcurrentHashMap<String, ThirdMonitorWarningHandleDetailResp>();
        List<CompletableFuture<Void>> futures = warningIds.stream()
                                                          .map(warningId -> CompletableFuture.runAsync(() -> {
                                                              if (StrUtil.isBlank(warningId)) {
                                                                  return;
                                                              }
                                                              ThirdMonitorWarningHandleDetailResp detailResp = fetchMonitorWarningHandleDetail(warningId);
                                                              if (detailResp != null) {
                                                                  detailMap.put(warningId, detailResp);
                                                              }
                                                          }, ThirdPartyWarningDataSplitSupport.WARNING_DETAIL_EXECUTOR))
                                                          .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return detailMap;
    }

    /**
     * 并发查询预警上下文对应的处置详情
     */
    Map<String, ThirdMonitorWarningHandleDetailResp> fetchMonitorWarningHandleDetailParallel(List<WarningContext> warningContexts) {
        if (warningContexts == null || warningContexts.isEmpty()) {
            return Map.of();
        }
        Map<String, ThirdMonitorWarningHandleDetailResp> detailMap = new ConcurrentHashMap<String, ThirdMonitorWarningHandleDetailResp>();
        List<CompletableFuture<Void>> futures = warningContexts.stream()
                                                               .map(ctx -> CompletableFuture.runAsync(() -> {
                                                                   String warningId = ctx.warningId();
                                                                   if (StrUtil.isBlank(warningId)) {
                                                                       return;
                                                                   }
                                                                   ThirdMonitorWarningHandleDetailResp detailResp = fetchMonitorWarningHandleDetail(warningId);
                                                                   if (detailResp != null) {
                                                                       detailMap.put(warningId, detailResp);
                                                                   }
                                                               }, ThirdPartyWarningDataSplitSupport.WARNING_DETAIL_EXECUTOR))
                                                               .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return detailMap;
    }

    /**
     * 并发查询监测点下的预警记录
     */
    List<MonitorPointWarnings> fetchMonitorPointWarningsParallel(List<ThirdMonitorPointResp> monitorPoints, ThirdPartyWarningDisposalBo bo) {
        if (monitorPoints == null || monitorPoints.isEmpty()) {
            return List.of();
        }
        LocalDateTime warningEndTime = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().resolveRemoteWarningEndTime(bo);
        LocalDateTime warningStartTime = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().resolveRemoteWarningStartTime(bo, warningEndTime);
        AtomicBoolean stopWarningFetch = new AtomicBoolean(false);
        ExecutorCompletionService<IndexedMonitorPointWarnings> completionService =
                new ExecutorCompletionService<IndexedMonitorPointWarnings>(ThirdPartyWarningDataSplitSupport.WARNING_LIST_EXECUTOR);
        List<Future<IndexedMonitorPointWarnings>> submittedFutures = new ArrayList<Future<IndexedMonitorPointWarnings>>();
        List<MonitorPointWarnings> orderedResults = new ArrayList<MonitorPointWarnings>(Collections.nCopies(monitorPoints.size(), null));
        int maxInFlight = Math.min(ThirdPartyWarningDataSplitSupport.WARNING_LIST_PARALLELISM, monitorPoints.size());
        int submitIndex = 0;
        int submitted = 0;
        int completed = 0;
        while (submitIndex < maxInFlight) {
            submittedFutures.add(submitMonitorPointWarningTask(
                    completionService,
                    monitorPoints.get(submitIndex),
                    submitIndex,
                    bo,
                    warningStartTime,
                    warningEndTime,
                    stopWarningFetch
            ));
            submitIndex++;
            submitted++;
        }
        while (completed < submitted && !stopWarningFetch.get()) {
            try {
                IndexedMonitorPointWarnings warnings = completionService.take().get();
                if (warnings != null && warnings.value() != null) {
                    orderedResults.set(warnings.index(), warnings.value());
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                ThirdPartyWarningDataSplitSupport.log.warn("并发获取监测点预警数据被中断，终止后续请求, error={}", ex.getMessage());
                break;
            } catch (Exception ex) {
                if (thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().isWarningSsoAuthFailure(ex)) {
                    stopWarningFetch.set(true);
                    cancelSubmittedFutures(submittedFutures);
                    ThirdPartyWarningDataSplitSupport.log.warn("三方监测预警凭证失效，终止后续监测点预警请求, completed={}, submitted={}, total={}, error={}",
                            completed, submitted, monitorPoints.size(), ex.getMessage());
                    break;
                }
                ThirdPartyWarningDataSplitSupport.log.warn("并发获取监测点预警数据future失败，降级为空结果, error={}", ex.getMessage());
            }
            completed++;
            if (!stopWarningFetch.get() && submitIndex < monitorPoints.size()) {
                submittedFutures.add(submitMonitorPointWarningTask(
                        completionService,
                        monitorPoints.get(submitIndex),
                        submitIndex,
                        bo,
                        warningStartTime,
                        warningEndTime,
                        stopWarningFetch
                ));
                submitIndex++;
                submitted++;
            }
        }
        return orderedResults.stream()
                             .map(result -> result == null ? new MonitorPointWarnings(null, List.of()) : result)
                             .toList();
    }

    /**
     * 拉取单个监测点预警并记录耗时，失败降级为空列表
     */
    JcdWarningFetchResult fetchWarningsForJcdId(String jcdId) {
        return fetchWarningsForJcdId(jcdId, () -> false);
    }

    JcdWarningFetchResult fetchWarningsForJcdId(String jcdId, BooleanSupplier stopRequested) {
        long fetchStartNanos = System.nanoTime();
        try {
            List<ThirdMonitorDeviceWarningResp> warnings = fetchAllMonitorDeviceWarnings(jcdId, null, null, stopRequested);
            long costMs = (System.nanoTime() - fetchStartNanos) / 1_000_000;
            return new JcdWarningFetchResult(jcdId, warnings, costMs);
        } catch (Exception e) {
            if (thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().isWarningSsoAuthFailure(e)) {
                ThirdPartyWarningDataSplitSupport.log.warn("拉取监测点预警数据认证失败，终止后续监测点预警请求, jcdId={}", jcdId);
                throw thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().toWarningSsoAuthException(e);
            }
            ThirdPartyWarningDataSplitSupport.log.warn("拉取监测点预警数据失败, jcdId={}", jcdId, e);
            long costMs = (System.nanoTime() - fetchStartNanos) / 1_000_000;
            return new JcdWarningFetchResult(jcdId, new ArrayList<ThirdMonitorDeviceWarningResp>(), costMs);
        }
    }

    /**
     * 拉取监测点下全部预警记录
     */
    List<ThirdMonitorDeviceWarningResp> fetchAllMonitorDeviceWarnings(String monitorPointId, Integer warningLevel, String disposalType) {
        LocalDateTime endTime = LocalDateTime.now(ThirdPartyWarningDataSplitSupport.BEIJING_ZONE_ID);
        return fetchAllMonitorDeviceWarnings(
                monitorPointId,
                warningLevel,
                disposalType,
                thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().resolveDefaultWarningStartTime(endTime),
                endTime
        );
    }

    List<ThirdMonitorDeviceWarningResp> fetchAllMonitorDeviceWarnings(
            String monitorPointId,
            Integer warningLevel,
            String disposalType,
            BooleanSupplier stopRequested
    ) {
        LocalDateTime endTime = LocalDateTime.now(ThirdPartyWarningDataSplitSupport.BEIJING_ZONE_ID);
        return fetchAllMonitorDeviceWarnings(
                monitorPointId,
                warningLevel,
                disposalType,
                thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().resolveDefaultWarningStartTime(endTime),
                endTime,
                stopRequested
        );
    }

    List<ThirdMonitorDeviceWarningResp> fetchAllMonitorDeviceWarnings(
            String monitorPointId,
            Integer warningLevel,
            String disposalType,
            LocalDateTime warningStartTime,
            LocalDateTime warningEndTime
    ) {
        return fetchAllMonitorDeviceWarnings(
                monitorPointId,
                warningLevel,
                disposalType,
                warningStartTime,
                warningEndTime,
                () -> false
        );
    }

    List<ThirdMonitorDeviceWarningResp> fetchAllMonitorDeviceWarnings(
            String monitorPointId,
            Integer warningLevel,
            String disposalType,
            LocalDateTime warningStartTime,
            LocalDateTime warningEndTime,
            BooleanSupplier stopRequested
    ) {
        if (StrUtil.isBlank(monitorPointId)) {
            return new ArrayList<ThirdMonitorDeviceWarningResp>();
        }

        List<ThirdMonitorDeviceWarningResp> result = new ArrayList<ThirdMonitorDeviceWarningResp>();
        int current = 1;
        while (true) {
            if (stopRequested != null && stopRequested.getAsBoolean()) {
                break;
            }
            ThirdMonitorDeviceWarningPageResp pageResp = fetchMonitorDeviceWarningPage(
                    monitorPointId,
                    null,
                    null,
                    warningLevel,
                    null,
                    disposalType,
                    current,
                    ThirdPartyWarningDataSplitSupport.THIRD_PARTY_PAGE_SIZE,
                    warningStartTime,
                    warningEndTime
            );
            if (pageResp == null || pageResp.getRecords() == null || pageResp.getRecords().isEmpty()) {
                break;
            }
            result.addAll(pageResp.getRecords());
            if (thirdPartyWarningDataSplitSupport.isLastPage(pageResp.getCurrent(), pageResp.getPages(), pageResp.getRecords().size())) {
                break;
            }
            current++;
        }
        return result;
    }

    /**
     * 获取三方预警监测点详情
     */
    ThirdMonitorDeviceWarningPageResp fetchMonitorDeviceWarningPage(
            String monitorPointId, Integer warningLevel, String disposalType, int current) {
        LocalDateTime endTime = LocalDateTime.now(ThirdPartyWarningDataSplitSupport.BEIJING_ZONE_ID);
        return fetchMonitorDeviceWarningPage(
                monitorPointId,
                null,
                null,
                warningLevel,
                null,
                disposalType,
                current,
                ThirdPartyWarningDataSplitSupport.THIRD_PARTY_PAGE_SIZE,
                thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().resolveDefaultWarningStartTime(endTime),
                endTime
        );
    }

    /**
     * 获取三方监测预警分页数据
     */
    ThirdMonitorDeviceWarningPageResp fetchMonitorDeviceWarningPage(
            String monitorPointId,
            String monitorPointName,
            String regionCode,
            Integer warningLevel,
            Integer validWarning,
            String disposalType,
            int current,
            int size,
            LocalDateTime warningStartTime,
            LocalDateTime warningEndTime
    ) {
        MonitorPoint localMonitorPoint = StrUtil.isBlank(monitorPointId) ? null : thirdPartyWarningDataSplitSupport.getMonitorPointMapper().selectById(monitorPointId);
        LocalDateTime queryEndTime = warningEndTime == null ? LocalDateTime.now(ThirdPartyWarningDataSplitSupport.BEIJING_ZONE_ID) : warningEndTime;
        LocalDateTime queryStartTime = warningStartTime == null ? thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().resolveDefaultWarningStartTime(queryEndTime) : warningStartTime;
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("jcdname", StrUtil.isBlank(monitorPointId) ? "" : StrUtil.blankToDefault(
                monitorPointName,
                localMonitorPoint == null ? "" : StrUtil.blankToDefault(localMonitorPoint.getMonitorName(), "")
        ));
        params.put("regionCode", thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().resolveMonitorWarningRegionCode(regionCode, localMonitorPoint));
        params.put("yjlevel", StrUtil.blankToDefault(thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().convertToThirdPartyWarningLevel(warningLevel), ""));
        params.put("sfwb", validWarning == null ? "" : (validWarning == 1 ? "0" : "1"));
        params.put("type", StrUtil.blankToDefault(disposalType, ""));
        params.put("size", size);
        params.put("current", current);
        params.put("yjStartTime", thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().formatWarningQueryTime(queryStartTime));
        params.put("yjEndTime", thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().formatWarningQueryTime(queryEndTime));

        int attempt = 1;
        while (true) {
            try {
                thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().ensureWarningSsoToken();
                ThirdPartyWarningDataResp resp = Req.post(thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().getMonitorWarningUrl())
                                                    .path("/dzapi/jcd/jcyjPage")
                                                    .header("sso", thirdPartyWarningDataSplitSupport.getToken())
                                                    .json(params)
                                                    .timeout(ThirdPartyWarningDataSplitSupport.WARNING_LIST_REQ_TIMEOUT)
                                                    .ok()
                                                    .obj(ThirdPartyWarningDataResp.class);
                if (ObjectUtil.isNull(resp) || !resp.isSuccess() || ObjectUtil.isNull(resp.getData())) {
                    ServiceException thirdPartyException = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().buildThirdPartyException("获取监测点预警数据失败", resp);
                    if (thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().isWarningSsoAuthFailure(resp)) {
                        throw new ThirdPartyWarningAuthException(thirdPartyException.getMessage(), thirdPartyException);
                    }
                    throw thirdPartyException;
                }
                return thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().parseMonitorDeviceWarningPageResp(resp.getData());
            } catch (Exception e) {
                if (thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().isWarningSsoAuthFailure(e)) {
                    ThirdPartyWarningDataSplitSupport.log.error("获取监测点预警数据认证失败，终止本批次后续请求, monitorPointId={}, current={}, error={}",
                            monitorPointId, current, e.getMessage());
                    throw thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().toWarningSsoAuthException(e);
                }
                if (thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().isConnectionResetException(e)) {
                    if (attempt < ThirdPartyWarningDataSplitSupport.WARNING_PAGE_MAX_RETRY) {
                        ThirdPartyWarningDataSplitSupport.log.warn("获取监测点预警数据连接重置，重试中, monitorPointId={}, current={}, attempt={}/{}",
                                monitorPointId, current, attempt, ThirdPartyWarningDataSplitSupport.WARNING_PAGE_MAX_RETRY);
                        attempt++;
                        continue;
                    }
                    ThirdPartyWarningDataSplitSupport.log.warn("获取监测点预警数据连接重置，重试后仍失败，按空结果降级, monitorPointId={}, current={}, error={}",
                            monitorPointId, current, e.getMessage());
                    return null;
                }
                if (thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().isNonJsonResponseException(e)) {
                    ThirdPartyWarningDataSplitSupport.log.warn("获取监测点预警数据返回非JSON，按空结果降级, monitorPointId={}, current={}, error={}",
                            monitorPointId, current, e.getMessage());
                    return null;
                }
                ThirdPartyWarningDataSplitSupport.log.error("获取监测点预警数据失败, monitorPointId={}: {}", monitorPointId, e.getMessage(), e);
                throw new ServiceException("获取监测点预警数据失败: " + e.getMessage());
            }
        }
    }

    private Future<IndexedMonitorPointWarnings> submitMonitorPointWarningTask(
            ExecutorCompletionService<IndexedMonitorPointWarnings> completionService,
            ThirdMonitorPointResp monitorPointResp,
            int index,
            ThirdPartyWarningDisposalBo bo,
            LocalDateTime warningStartTime,
            LocalDateTime warningEndTime,
            AtomicBoolean stopWarningFetch
    ) {
        return completionService.submit(() -> {
            if (stopWarningFetch.get()) {
                return new IndexedMonitorPointWarnings(index, new MonitorPointWarnings(monitorPointResp, List.of()));
            }
            try {
                return new IndexedMonitorPointWarnings(
                        index,
                        new MonitorPointWarnings(
                                monitorPointResp,
                                fetchAllMonitorDeviceWarnings(
                                        monitorPointResp == null ? null : monitorPointResp.getId(),
                                        bo.getWarningLevel(),
                                        bo.getDisposalType(),
                                        warningStartTime,
                                        warningEndTime,
                                        stopWarningFetch::get
                                )
                        )
                );
            } catch (Exception ex) {
                if (thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().isWarningSsoAuthFailure(ex)) {
                    stopWarningFetch.set(true);
                    throw thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().toWarningSsoAuthException(ex);
                }
                ThirdPartyWarningDataSplitSupport.log.warn("并发获取监测点预警数据失败，降级为空结果, monitorPointId={}, error={}",
                        monitorPointResp == null ? null : monitorPointResp.getId(), ex.getMessage());
                return new IndexedMonitorPointWarnings(index, new MonitorPointWarnings(monitorPointResp, List.of()));
            }
        });
    }

    private void cancelSubmittedFutures(List<? extends Future<?>> futures) {
        for (Future<?> future : futures) {
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        }
    }

    private record IndexedMonitorPointWarnings(int index, MonitorPointWarnings value) {
    }

    /**
     * 获取单条预警处置详情
     */
    ThirdMonitorWarningHandleDetailResp fetchMonitorWarningHandleDetail(String warningId) {
        if (StrUtil.isBlank(warningId)) {
            return null;
        }

        try {
            thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().ensureWarningSsoToken();
            ThirdPartyWarningDataResp resp = Req.get(thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().getMonitorWarningUrl())
                                                .path("/dzapi/jcd/jcyjDetail")
                                                .header("sso", thirdPartyWarningDataSplitSupport.getToken())
                                                .query("jcdyjid", warningId)
                                                .timeout(ThirdPartyWarningDataSplitSupport.WARNING_DETAIL_REQ_TIMEOUT)
                                                .ok()
                                                .obj(ThirdPartyWarningDataResp.class);
            if (ObjectUtil.isNull(resp) || !resp.isSuccess() || ObjectUtil.isNull(resp.getData())) {
                ThirdPartyWarningDataSplitSupport.log.warn("获取监测预警处置详情为空, warningId={}", warningId);
                return null;
            }
            return JSONUtil.toBean(JSONUtil.parseObj(resp.getData()), ThirdMonitorWarningHandleDetailResp.class);
        } catch (Exception e) {
            ThirdPartyWarningDataSplitSupport.log.warn("获取监测预警处置详情失败, warningId={}, error={}", warningId, e.getMessage());
            return null;
        }
    }
}
