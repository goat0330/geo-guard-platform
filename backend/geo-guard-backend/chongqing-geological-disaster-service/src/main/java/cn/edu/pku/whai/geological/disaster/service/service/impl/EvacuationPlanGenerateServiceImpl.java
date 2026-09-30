/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.data.domain.po.EvacuationPlan;
import cn.edu.pku.whai.geological.disaster.data.domain.po.House;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HazardPointVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorPointVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGridMemberRelationVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.DataHouseMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.DzResettlementInfoMapper;
import cn.edu.pku.whai.geological.disaster.data.mapper.EvacuationPlanMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IHazardPointService;
import cn.edu.pku.whai.geological.disaster.data.service.IMonitorPointService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailBizTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DetailContentTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.PlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.dify.DifyAgentClient;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentStepsBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessment;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.req.GenerateEvacuationPlanReq;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.*;
import cn.edu.pku.whai.geological.disaster.service.handle.client.HandleProcessApiClient;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.service.*;
import cn.edu.pku.whai.geological.disaster.service.utils.EvacuationSchemeParser;
import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.github.imfangs.dify.client.DifyChatflowClient;
import io.github.imfangs.dify.client.callback.ChatflowStreamCallback;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.event.MessageEvent;
import io.github.imfangs.dify.client.event.WorkflowFinishedEvent;
import io.github.imfangs.dify.client.model.chat.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.*;

/**
 * 撤离方案生成服务实现
 * 通过handle_id从dz_task_handle_detail_content聚合获取ai_report作为文本，
 * 从dz_task_handle_scene_record表构建plan_types，调用外部API生成撤离方案
 *
 * @author whai
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class EvacuationPlanGenerateServiceImpl implements IEvacuationPlanGenerateService {

    private static final String ENSHI_REGION_PREFIX = "湖北省恩施土家族苗族自治州";
    private static final String ENSHI_BUILDING_NAME_PREFIX = ENSHI_REGION_PREFIX + "恩施市";

    private final IDzTaskHandleSceneRecordService dzTaskHandleSceneRecordService;
    private final HandleProcessApiClient handleProcessApiClient;
    private final IDzTaskHandleDetailContentService dzTaskHandleDetailContentService;
    private final DzTaskHandleMapper dzTaskHandleMapper;
    private final EvacuationPlanMapper evacuationPlanMapper;
    private final DataHouseMapper dataHouseMapper;
    private final DzResettlementInfoMapper dzResettlementInfoMapper;
    private final DifyAgentClient difyAgentClient;
    private final TransactionTemplate transactionTemplate;
    private final IDzSingleDefProgressService dzSingleDefProgressService;
    private final DzRiskAssessmentMapper dzRiskAssessmentMapper;
    private final IDzRiskAssessmentStepsService dzRiskAssessmentStepsService;
    private final ISlopeUnitService slopeUnitService;
    private final ISlopeUnitGridMemberRelationService slopeUnitGridMemberRelationService;
    private final IHazardPointService hazardPointService;
    private final IMonitorPointService monitorPointService;

    @Override
    public void generateOrModifyEvacuationPlanStream(SseEmitter sseEmitter, GenerateEvacuationPlanReq req) {
        if (req == null || req.getHandleId() == null) {
            throw new ServiceException("handleId不能为空");
        }
        Long handleId = req.getHandleId();
        DzTaskHandleSceneRecordVo sceneRecord = dzTaskHandleSceneRecordService.getByHandleId(handleId);
        if (sceneRecord == null) {
            throw new ServiceException("未找到handleId对应的现场记录");
        }
        int currentRoundNo = resolveCurrentRoundNo(handleId);
        DzTaskHandleDetailContentVo currentRoundPlan = queryLatestTaskHandleContent(
            handleId,
            DetailContentTypeEnum.EVACUATION_PLAN.getCode(),
            currentRoundNo
        );
        boolean hasExistingPlan = currentRoundPlan != null && StringUtils.isNotBlank(currentRoundPlan.getPlanContent());
        if (hasExistingPlan && StringUtils.isNotBlank(req.getExpertOpinion())) {
            dzTaskHandleDetailContentService.updateLatestSuggest(
                DetailBizTypeEnum.TASK_HANDLE.getCode(),
                handleId,
                DetailContentTypeEnum.EVACUATION_PLAN.getCode(),
                req.getExpertOpinion()
            );
        }
        DzTaskHandle taskHandle = dzTaskHandleMapper.selectById(handleId);
        DifyChatflowClient agent = difyAgentClient.getAiEvacuationPlanAgent();
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new ServiceException("当前登录用户不存在，无法生成撤离方案");
        }
        Long currentUserId = loginUser.getUserId();
        String currentUserName = StringUtils.defaultIfBlank(loginUser.getNickname(), loginUser.getUsername());
        if (StringUtils.isBlank(currentUserName)) {
            throw new ServiceException("当前登录用户名不能为空，无法生成撤离方案");
        }
        Map<String, Object> structuredInputs = buildStructuredInputs(taskHandle);
        Map<String, Object> inputs = MapUtil.<String, Object>builder()
                                            .put("text", wrapTripleQuotes(getLatestAiReportText(handleId)))
                                            .put("plan_types", buildQuotedPlanTypesFromMeasures(sceneRecord))
                                            .put("schemes", hasExistingPlan ? getSchemeText(req, currentRoundPlan) : "")
                                            .put("expert_opinion", hasExistingPlan ? (req.getExpertOpinion() == null ? "" : req.getExpertOpinion()) : "")
                                            .put("slope_info", JsonUtils.toJsonString(structuredInputs.get("slope_info")))
                                            .put("hazard_info", JsonUtils.toJsonString(structuredInputs.get("hazard_info")))
                                            .put("monitor_info", JsonUtils.toJsonString(structuredInputs.get("monitor_info")))
                                            .put("resource_director", structuredInputs.get("resource_director"))
                                            .put("resource_director_phone", structuredInputs.get("resource_director_phone"))
                                            .build();
        ChatMessage message = ChatMessage.builder()
                                         .query(buildQueryText(hasExistingPlan, req.getExpertOpinion()))
                                         .inputs(inputs)
                                         .responseMode(ResponseMode.STREAMING)
                                         .user("admin")
                                         .conversationId("")
                                         .build();
        try {
            agent.sendChatMessageStream(message, new ChatflowStreamCallback() {
                @Override
                public void onMessage(MessageEvent event) {
                    sendEmitter(sseEmitter, event);
                }

                @Override
                public void onWorkflowFinished(WorkflowFinishedEvent event) {
                    try {
                        Object answer = event == null || event.getData() == null || event.getData().getOutputs() == null
                            ? null
                            : event.getData().getOutputs().get("answer");
                        if (answer == null) {
                            throw new ServiceException("AI未返回撤离方案内容");
                        }
                        String content = answer.toString();
                        String planContentJson = EvacuationSchemeParser.toJson(content);
                        dzTaskHandleDetailContentService.saveEvacuationPlanContent(handleId, currentRoundNo, content, planContentJson, currentUserId, currentUserName);
                        dzSingleDefProgressService.recordPlanGenerated(handleId, new Date());
                    } catch (ServiceException e) {
                        sseEmitter.completeWithError(e);
                        return;
                    }
                    sendEmitter(sseEmitter, event);
                    sseEmitter.complete();
                }
            });
        } catch (Exception e) {
            throw new ServiceException("调用Dify流式接口失败");
        }
    }

    @Override
    public EvacuationRouteResultVo generateEvacuationRoute(Long handleId) {
        try {
            return transactionTemplate.execute(status -> doGenerateEvacuationRoute(handleId));
        } catch (RuntimeException e) {
            log.error("生成疏散路线失败, handleId={}", handleId, e);
            throw e;
        }
    }

    private EvacuationRouteResultVo doGenerateEvacuationRoute(Long handleId) {
        DzTaskHandle taskHandle = dzTaskHandleMapper.selectById(handleId);
        if (taskHandle == null) {
            throw new ServiceException("未找到handleId对应的处置任务");
        }
        String slopeUnitId = taskHandle.getSlopeUnitId();
        if (StringUtils.isBlank(slopeUnitId)) {
            throw new ServiceException("处置任务未关联斜坡单元");
        }
        DzTaskHandleSceneRecordVo sceneRecord = dzTaskHandleSceneRecordService.getByHandleId(handleId);
        if (sceneRecord == null) {
            throw new ServiceException("未找到handleId对应的现场记录");
        }
        Map<String, Object> hazardPoints = buildHazardPointsFromHouses(sceneRecord);
        if (hazardPoints.isEmpty()) {
            throw new ServiceException("风险区域到未匹配到建筑坐标");
        }
        EvacuationRouteResultVo routeResult = handleProcessApiClient.evacuationRoute(handleId, slopeUnitId, hazardPoints, sceneRecord.getRiskExtent());
        saveEvacuationPlans(handleId, sceneRecord, routeResult);
        return routeResult;
    }

    private void saveEvacuationPlans(Long handleId, DzTaskHandleSceneRecordVo sceneRecord, EvacuationRouteResultVo routeResult) {
        if (routeResult == null || routeResult.getRoutes() == null || routeResult.getRoutes().isEmpty()) {
            throw new ServiceException(routeResult.getSkipped().getFirst().getReason());
        }
        String evacuationAreaWkt = resolveEvacuationAreaWkt(sceneRecord);
        evacuationPlanMapper.deleteByHandleId(handleId);
        for (RouteItem route : routeResult.getRoutes()) {
            if (route == null) {
                continue;
            }
            EvacuationPlan plan = buildEvacuationPlan(handleId, route, evacuationAreaWkt);
            evacuationPlanMapper.insertPlan(plan);
        }
    }

    private EvacuationPlan buildEvacuationPlan(Long handleId, RouteItem route, String evacuationAreaWkt) {
        EvacuationPlan plan = new EvacuationPlan();
        plan.setHandleId(handleId);
        plan.setEvacuationArea(route.getHazardLabel());
        plan.setEvacuationAreaWkt(evacuationAreaWkt);
        plan.setResettlementLocation(route.getResettlementLocation());
        plan.setDistanceKm(toBigDecimal(route.getDistanceKm()));
        plan.setEstimatedTimeMinutes(route.getEstimatedTimeMinutes());
        plan.setEvacuationRoute(route.getEvacuationRoute());
        plan.setEvacuationRoad(route.getPathWkt());
        plan.setEvacuationAreaToRoad(resolveEvacuationAreaToRoad(route));
        plan.setRoadToResettlement(resolveRoadToResettlement(route));
        plan.setCreateTime(new Date());
        plan.setResettlementWkt(resolveResettlementWkt(route.getResettlementWkt(), route.getResettlementLocation(), route.getResettlementPoint()));
        plan.setResettlementPoint(route.getResettlementPoint() == null ? null : JsonUtils.toJsonString(route.getResettlementPoint()));
        plan.setResidentsInfo(route.getResidentsInfo() == null ? null : JsonUtils.toJsonString(route.getResidentsInfo()));
        return plan;
    }

    private String resolveEvacuationAreaWkt(DzTaskHandleSceneRecordVo sceneRecord) {
        if (sceneRecord == null || StringUtils.isBlank(sceneRecord.getRiskExtent())) {
            throw new ServiceException("未解析到有效灾害范围，无法保存撤离方案");
        }
        return sceneRecord.getRiskExtent().trim();
    }

    private String resolveResettlementWkt(String routeResettlementWkt, String resettlementLocation, EvacuationRouteResultVo.GeoPoint point) {
        if (StringUtils.isNotBlank(routeResettlementWkt)) {
            return routeResettlementWkt.trim();
        }
        if (StringUtils.isNotBlank(resettlementLocation)) {
            String configuredWkt = dzResettlementInfoMapper.selectWktByName(resettlementLocation);
            if (StringUtils.isNotBlank(configuredWkt)) {
                return configuredWkt.trim();
            }
            log.warn("三方未返回安置点WKT且安置点未配置WKT，回退为20m矩形范围，name={}", resettlementLocation);
        }
        if (point == null || point.getLon() == null || point.getLat() == null) {
            throw new ServiceException("安置点WKT和坐标均缺失，无法保存撤离方案");
        }
        log.warn("三方未返回安置点WKT且未匹配到预置WKT，回退为20m矩形范围，name={}, lon={}, lat={}", resettlementLocation, point.getLon(), point.getLat());
        double lon = point.getLon();
        double lat = point.getLat();
        // 按20m*20m范围兜底，中心点向四周各扩10m
        double latDelta = 10D / 111320D;
        double cosLat = Math.cos(Math.toRadians(lat));
        double lonDelta = 10D / (111320D * (Math.abs(cosLat) < 1E-8 ? 1E-8 : cosLat));
        return buildRectangleWkt(
            toBigDecimal(lon - lonDelta),
            toBigDecimal(lat - latDelta),
            toBigDecimal(lon + lonDelta),
            toBigDecimal(lat + latDelta)
        );
    }

    private String buildRectangleWkt(BigDecimal lonMin, BigDecimal latMin, BigDecimal lonMax, BigDecimal latMax) {
        if (lonMin == null || latMin == null || lonMax == null || latMax == null) {
            return null;
        }
        return "POLYGON(("
            + lonMin.toPlainString() + " " + latMin.toPlainString() + ", "
            + lonMax.toPlainString() + " " + latMin.toPlainString() + ", "
            + lonMax.toPlainString() + " " + latMax.toPlainString() + ", "
            + lonMin.toPlainString() + " " + latMax.toPlainString() + ", "
            + lonMin.toPlainString() + " " + latMin.toPlainString()
            + "))";
    }

    private String resolveEvacuationAreaToRoad(RouteItem route) {
        if (route == null) {
            return null;
        }
        return buildConnectorLineWkt(route.getHazardPoint(), resolveRouteBoundaryPoint(route, true));
    }

    private String resolveRoadToResettlement(RouteItem route) {
        if (route == null) {
            return null;
        }
        return buildConnectorLineWkt(resolveRouteBoundaryPoint(route, false), route.getResettlementPoint());
    }

    private EvacuationRouteResultVo.GeoPoint resolveRouteBoundaryPoint(RouteItem route, boolean start) {
        if (route == null) {
            return null;
        }
        // 连接线严格按 path_wkt 首尾点生成，不使用 path 数组兜底，避免与三方主路径不一致。
        return parseLineBoundaryPoint(route.getPathWkt(), start);
    }

    private EvacuationRouteResultVo.GeoPoint parseLineBoundaryPoint(String lineWkt, boolean start) {
        if (StringUtils.isBlank(lineWkt)) {
            return null;
        }
        String normalized = lineWkt.trim();
        String upper = normalized.toUpperCase(Locale.ROOT);
        String body;
        if (upper.startsWith("LINESTRING(") && normalized.endsWith(")")) {
            body = normalized.substring("LINESTRING(".length(), normalized.length() - 1);
        } else if (upper.startsWith("MULTILINESTRING((") && normalized.endsWith("))")) {
            body = normalized.substring("MULTILINESTRING((".length(), normalized.length() - 2)
                             .replace("),(", ",");
        } else {
            return null;
        }
        String[] points = body.split(",");
        if (points.length == 0) {
            return null;
        }
        String targetPoint = start ? points[0] : points[points.length - 1];
        String[] values = targetPoint.trim().split("\\s+");
        if (values.length < 2) {
            return null;
        }
        try {
            EvacuationRouteResultVo.GeoPoint point = new EvacuationRouteResultVo.GeoPoint();
            point.setLon(Double.parseDouble(values[0]));
            point.setLat(Double.parseDouble(values[1]));
            return point;
        } catch (NumberFormatException e) {
            log.warn("解析撤离路线端点失败，lineWkt={}", lineWkt, e);
            return null;
        }
    }

    private String buildConnectorLineWkt(EvacuationRouteResultVo.GeoPoint from, EvacuationRouteResultVo.GeoPoint to) {
        if (from == null || to == null || from.getLon() == null || from.getLat() == null || to.getLon() == null || to.getLat() == null) {
            return null;
        }
        if (isSamePoint(from, to)) {
            return null;
        }
        BigDecimal fromLon = toBigDecimal(from.getLon());
        BigDecimal fromLat = toBigDecimal(from.getLat());
        BigDecimal toLon = toBigDecimal(to.getLon());
        BigDecimal toLat = toBigDecimal(to.getLat());
        return "MULTILINESTRING(("
            + fromLon.toPlainString() + " " + fromLat.toPlainString() + ", "
            + toLon.toPlainString() + " " + toLat.toPlainString()
            + "))";
    }

    private boolean isSamePoint(EvacuationRouteResultVo.GeoPoint left, EvacuationRouteResultVo.GeoPoint right) {
        if (left == null || right == null || left.getLon() == null || left.getLat() == null || right.getLon() == null || right.getLat() == null) {
            return false;
        }
        return Math.abs(left.getLon() - right.getLon()) < 1E-8
            && Math.abs(left.getLat() - right.getLat()) < 1E-8;
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private DzTaskHandleDetailContentVo queryLatestTaskHandleContent(Long handleId, Integer contentType) {
        return queryLatestTaskHandleContent(handleId, contentType, null);
    }

    private DzTaskHandleDetailContentVo queryLatestTaskHandleContent(Long handleId, Integer contentType, Integer roundNo) {
        DzTaskHandleDetailContentVo latest = dzTaskHandleDetailContentService.queryLatest(
            DetailBizTypeEnum.TASK_HANDLE.getCode(),
            handleId,
            contentType,
            null
        );
        if (latest == null) {
            return null;
        }
        return roundNo == null || roundNo.equals(latest.getRoundNo()) ? latest : null;
    }

    private String getLatestAiReportText(Long handleId) {
        DzTaskHandleDetailContentVo detail = queryLatestTaskHandleContent(handleId, DetailContentTypeEnum.AI_REPORT.getCode());
        String text = detail == null ? null : detail.getPlanContent();
        if (StringUtils.isBlank(text)) {
            throw new ServiceException("处置详情中ai_report为空，无法生成撤离方案");
        }
        return text;
    }

    private int resolveCurrentRoundNo(Long handleId) {
        DzTaskHandleDetailContentVo latestAiReport = queryLatestTaskHandleContent(handleId, DetailContentTypeEnum.AI_REPORT.getCode());
        if (latestAiReport != null && latestAiReport.getRoundNo() != null) {
            return latestAiReport.getRoundNo();
        }
        DzTaskHandleDetailContentVo latestPlan = queryLatestTaskHandleContent(handleId, DetailContentTypeEnum.EVACUATION_PLAN.getCode());
        return latestPlan != null && latestPlan.getRoundNo() != null ? latestPlan.getRoundNo() : 1;
    }

    private String getSchemeText(GenerateEvacuationPlanReq req, DzTaskHandleDetailContentVo currentRoundPlan) {
        if (StringUtils.isNotBlank(req.getSchemes())) {
            return req.getSchemes();
        }
        return currentRoundPlan.getPlanContent() == null ? "" : currentRoundPlan.getPlanContent();
    }

    private String buildQueryText(boolean hasExistingPlan, String expertOpinion) {
        if (!hasExistingPlan) {
            return "生成撤离方案";
        }
        if (StringUtils.isNotBlank(expertOpinion)) {
            return expertOpinion;
        }
        return "根据现有方案优化撤离方案";
    }

    private Map<String, Object> buildStructuredInputs(DzTaskHandle taskHandle) {
        String slopeUnitId = taskHandle == null ? null : trimToNull(taskHandle.getSlopeUnitId());
        Map<String, String> resourceDirectorInfo = buildResourceDirectorInfo(taskHandle, slopeUnitId);
        return MapUtil.<String, Object>builder()
                      .put("slope_info", buildSlopeInfo(slopeUnitId))
                      .put("hazard_info", buildHazardInfo(slopeUnitId))
                      .put("monitor_info", buildMonitorInfo(slopeUnitId, resourceDirectorInfo))
                      .put("resource_director", resourceDirectorInfo.get("resource_director"))
                      .put("resource_director_phone", resourceDirectorInfo.get("resource_director_phone"))
                      .build();
    }

    private Map<String, Object> buildSlopeInfo(String slopeUnitId) {
        DzRiskAssessmentStepsVo latestSteps = queryLatestRiskAssessmentSteps(slopeUnitId);
        if (latestSteps == null) {
            return Map.of();
        }
        return MapUtil.<String, Object>builder()
                      .put("slope_structure", latestSteps.getSlopeStructure())
                      .put("elevation_mean", latestSteps.getElevationMean())
                      .put("elevation_max", latestSteps.getElevationMax())
                      .put("elevation_min", latestSteps.getElevationMin())
                      .put("elevation_diff", latestSteps.getElevationDiff())
                      .put("slope_mean", latestSteps.getSlopeMean())
                      .put("aspect_mean", latestSteps.getAspectMean())
                      .put("plan_curvature", latestSteps.getPlanCurvature())
                      .put("profile_curvature", latestSteps.getProfileCurvature())
                      .put("lithology_desc", latestSteps.getLithologyDesc())
                      .put("unit_morphology", latestSteps.getUnitMorphology())
                      .put("vegetation_cover", latestSteps.getVegetationCover())
                      .put("adjacent_water", latestSteps.getAdjacentWater())
                      .put("tectonic_dist", latestSteps.getTectonicDist())
                      .put("rainfall_past7_daily", latestSteps.getRainfallPast7Daily() == null ? List.of() : latestSteps.getRainfallPast7Daily())
                      .build();
    }

    private DzRiskAssessmentStepsVo queryLatestRiskAssessmentSteps(String slopeUnitId) {
        if (StringUtils.isBlank(slopeUnitId)) {
            return null;
        }
        DzRiskAssessment latestAssessment = dzRiskAssessmentMapper.selectOne(new QueryWrapper<DzRiskAssessment>()
            .eq("slope_unit_id", slopeUnitId)
            .orderByDesc("create_date", "id")
            .last("limit 1"));
        if (latestAssessment == null || latestAssessment.getId() == null) {
            return null;
        }
        DzRiskAssessmentStepsBo bo = new DzRiskAssessmentStepsBo();
        bo.setAssessmentId(latestAssessment.getId());
        List<DzRiskAssessmentStepsVo> steps = dzRiskAssessmentStepsService.queryList(bo);
        return (steps == null || steps.isEmpty()) ? null : steps.getLast();
    }

    private List<Map<String, Object>> buildHazardInfo(String slopeUnitId) {
        if (StringUtils.isBlank(slopeUnitId)) {
            return List.of();
        }
        List<HazardPointVo> hazardPoints = hazardPointService.queryBySlopeUnitId(slopeUnitId, null);
        if (hazardPoints == null || hazardPoints.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>(hazardPoints.size());
        for (HazardPointVo item : hazardPoints) {
            if (item == null) {
                continue;
            }
            result.add(MapUtil.<String, Object>builder()
                              .put("hazard_name", item.getName())
                              .put("hazard_type", item.getTypeCode())
                              .put("hazard_code", item.getUniqueDisasterId())
                              .put("location", joinLocation(item.getProvince(), item.getCity(), item.getCounty(), item.getStreet(), item.getVillage()))
                              .put("longitude", item.getLongitude())
                              .put("latitude", item.getLatitude())
                              .put("scale_grade", item.getScaleGrade())
                              .put("threatened_population", item.getThreatenedPopulation())
                              .put("threatened_property_value", item.getThreatenedPropertyValue())
                              .put("risk_grade", item.getRiskGrade())
                              .put("geological_environment", item.getGeologicalEnvironment())
                              .put("deformation_features", item.getDeformationFeatures())
                              .put("stability_analysis", item.getStabilityAnalysis())
                              .put("stability_status", item.getStabilityStatus())
                              .put("stability_trend", item.getStabilityTrend())
                              .put("trigger_factors", item.getTriggerFactors())
                              .put("potential_hazards", item.getPotentialHazards())
                              .put("pre_disaster_prediction", item.getPreDisasterPrediction())
                              .put("disaster_history_time", item.getDisasterHistoryTime())
                              .build());
        }
        return result;
    }

    private List<Map<String, Object>> buildMonitorInfo(String slopeUnitId, Map<String, String> resourceDirectorInfo) {
        if (StringUtils.isBlank(slopeUnitId)) {
            return List.of();
        }
        List<MonitorPointVo> monitorPoints = monitorPointService.queryBySlopeUnitId(slopeUnitId);
        if (monitorPoints == null || monitorPoints.isEmpty()) {
            return List.of();
        }
        String responsiblePerson = resourceDirectorInfo == null ? "" : defaultString(resourceDirectorInfo.get("resource_director"));
        String responsiblePersonPhone = resourceDirectorInfo == null ? "" : defaultString(resourceDirectorInfo.get("resource_director_phone"));
        List<Map<String, Object>> result = new ArrayList<>(monitorPoints.size());
        for (MonitorPointVo item : monitorPoints) {
            if (item == null) {
                continue;
            }
            result.add(MapUtil.<String, Object>builder()
                              .put("monitor_id", item.getId())
                              .put("monitor_code", item.getMonitorCode())
                              .put("monitor_name", item.getMonitorName())
                              .put("location_desc", item.getLocationDesc())
                              .put("longitude", item.getLongitude())
                              .put("latitude", item.getLatitude())
                              .put("altitude", item.getAltitude())
                              .put("construction_unit", item.getConstructionUnit())
                              .put("responsible_department", item.getResponsibleDepartment())
                              .put("operation_maintenance_unit", item.getOperationMaintenanceUnit())
                              .put("responsible_person", responsiblePerson)
                              .put("responsible_person_phone", responsiblePersonPhone)
                              .build());
        }
        return result;
    }

    private Map<String, String> buildResourceDirectorInfo(DzTaskHandle taskHandle, String slopeUnitId) {
        if (StringUtils.isBlank(slopeUnitId)) {
            return MapUtil.<String, String>builder()
                          .put("resource_director", "")
                          .put("resource_director_phone", "")
                          .build();
        }
        SlopeUnitGridMemberRelationVo relationVo = slopeUnitGridMemberRelationService.queryByUnitId(slopeUnitId.trim());
        if (relationVo == null) {
            return MapUtil.<String, String>builder()
                          .put("resource_director", "")
                          .put("resource_director_phone", "")
                          .build();
        }
        return MapUtil.<String, String>builder()
                      .put("resource_director", defaultString(relationVo.getAdminUser()))
                      .put("resource_director_phone", defaultString(relationVo.getAdminUserPhone()))
                      .build();
    }

    private String joinLocation(String province, String city, String county, String street, String village) {
        return String.join("",
            defaultString(province),
            defaultString(city),
            defaultString(county),
            defaultString(street),
            defaultString(village)
        );
    }

    private String trimToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private Map<String, Object> buildHazardPointsFromHouses(DzTaskHandleSceneRecordVo sceneRecord) {
        String polygonWkt = sceneRecord.getRiskExtent();
        QueryWrapper<House> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNotNull("longitude").isNotNull("latitude");
        queryWrapper.and(wrapper -> wrapper.apply(
            "ST_Contains(ST_GeomFromText({0}, 4490), ST_SetSRID(ST_MakePoint(longitude::double precision, latitude::double precision), 4490))",
            polygonWkt
        ));
        List<House> houses = dataHouseMapper.selectList(queryWrapper);
        if (houses == null || houses.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> hazardPoints = new LinkedHashMap<>();
        int pointIndex = 1;
        for (House house : houses) {
            if (house == null || house.getLongitude() == null || house.getLatitude() == null) {
                continue;
            }
            String pointName = StringUtils.isNotBlank(house.getBuildingName())
                ? stripBuildingNameForLlm(house.getBuildingName())
                : ("风险点" + pointIndex);
            String uniquePointName = pointName;
            int duplicateIndex = 2;
            while (hazardPoints.containsKey(uniquePointName)) {
                uniquePointName = pointName + "(" + duplicateIndex + ")";
                duplicateIndex++;
            }
            hazardPoints.put(uniquePointName, Map.of(
                "lon", house.getLongitude().doubleValue(),
                "lat", house.getLatitude().doubleValue()
            ));
            pointIndex++;
        }
        return hazardPoints;
    }

    private String stripBuildingNameForLlm(String buildingName) {
        if (StringUtils.isBlank(buildingName)) {
            return buildingName;
        }
        String trimmed = buildingName.trim();
        if (trimmed.startsWith(ENSHI_BUILDING_NAME_PREFIX)) {
            return trimmed.substring(ENSHI_BUILDING_NAME_PREFIX.length()).trim();
        }
        if (trimmed.startsWith(ENSHI_REGION_PREFIX)) {
            return trimmed.substring(ENSHI_REGION_PREFIX.length()).trim();
        }
        return trimmed;
    }

    private List<String> buildPlanTypesFromMeasures(DzTaskHandleSceneRecordVo vo) {
        List<String> planTypes = new ArrayList<>();
        if (StringUtils.isNotBlank(vo.getMeasuresEvacuation())) {
            planTypes.add(PlanTypeEnum.RESETTLEMENT.getName());
        }
        if (StringUtils.isNotBlank(vo.getMeasuresProtection())) {
            planTypes.add(PlanTypeEnum.PROTECTION.getName());
        }
        if (StringUtils.isNotBlank(vo.getMeasuresMonitoring())) {
            planTypes.add(PlanTypeEnum.MONITORING.getName());
        }
        if (StringUtils.isNotBlank(vo.getMeasuresHazardRemoval())) {
            planTypes.add(PlanTypeEnum.HAZARD_REMOVAL.getName());
        }
        if (StringUtils.isNotBlank(vo.getMeasuresEngineering())) {
            planTypes.add(PlanTypeEnum.ENGINEERING.getName());
        }
        if (StringUtils.isNotBlank(vo.getMeasuresPublicity())) {
            planTypes.add(PlanTypeEnum.PUBLICITY.getName());
        }
        if (StringUtils.isNotBlank(vo.getMeasuresTrafficControl())) {
            planTypes.add(PlanTypeEnum.TRAFFIC_CONTROL.getName());
        }
        if (StringUtils.isNotBlank(vo.getMeasuresOtherSuggestions())) {
            planTypes.add(PlanTypeEnum.OTHER_SUGGESTIONS.getName());
        }
        return planTypes;
    }

    private String buildQuotedPlanTypesFromMeasures(DzTaskHandleSceneRecordVo vo) {
        List<String> planTypes = buildPlanTypesFromMeasures(vo);
        if (planTypes.isEmpty()) {
            return "[]";
        }
        List<String> quoted = new ArrayList<>(planTypes.size());
        for (String item : planTypes) {
            quoted.add("\"" + item + "\"");
        }
        return "[" + String.join(", ", quoted) + "]";
    }

    private String wrapTripleQuotes(String text) {
        return "\"\"\"" + (text == null ? "" : text) + "\"\"\"";
    }

    private void sendEmitter(SseEmitter emitter, Object vo) {
        try {
            emitter.send(SseEmitter.event().comment("message").data(JsonUtils.toJsonString(vo)));
        } catch (Exception e) {
            log.error("发送sse消息失败", e);
        }
    }

}
