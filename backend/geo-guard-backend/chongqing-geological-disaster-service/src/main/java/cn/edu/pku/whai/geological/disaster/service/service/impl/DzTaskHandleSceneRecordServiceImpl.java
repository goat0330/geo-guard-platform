package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.DzTaskHandleSceneRecordReq;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.EventTypeEnum;
import cn.edu.pku.whai.geological.disaster.data.utils.JacksonUtil;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleSceneRecordBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandleSceneRecord;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleSceneRecordVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleSceneRecordMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleSceneRecordService;
import cn.edu.pku.whai.geological.disaster.service.sse.service.AiHostingOverviewNotifyService;
import cn.edu.pku.whai.geological.disaster.service.utils.AiReportEventLevelSupport;
import cn.edu.pku.whai.geological.disaster.service.utils.CoordinateConverter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 处置管理现场记录Service业务层处理
 *
 * @author kongweiguang
 * @date 2026-01-30
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DzTaskHandleSceneRecordServiceImpl implements IDzTaskHandleSceneRecordService {

    private final DzTaskHandleSceneRecordMapper baseMapper;
    private final DzTaskHandleMapper dzTaskHandleMapper;
    private final AiHostingOverviewNotifyService aiHostingOverviewNotifyService;
    private static final int TAKEN_MEASURES_MAX_LENGTH = 500;
    private static final Map<String, List<String>> SPECIAL_REQUIRED_FIELDS = Map.of(
        "滑坡", List.of("landslide_boundary", "sliding_mass", "sliding_surface", "deformation_stage"),
        "崩塌", List.of("collapse_body_type", "main_collapse_direction", "deformation_failure_mode",
            "collapse_source_area_feature", "collapse_path_feature", "collapse_deposit_area_feature", "deformation_stage"),
        "泥石流", List.of("material_composition", "source_supply_path", "catchment_area",
            "source_area_feature", "flow_area_feature", "fan_deposit_area_feature", "deformation_stage"),
        "地面塌陷", List.of("collapse_cause_type", "pit_long_axis_direction", "pit_shape",
            "collapse_geological_model", "deformation_feature", "deformation_stage")
    );

    /**
     * 查询处置管理现场记录
     *
     * @param id 主键
     * @return 处置管理现场记录
     */
    @Override
    public DzTaskHandleSceneRecordVo queryById(Long id) {
        return fillCompatibleFields(baseMapper.selectVoById(id));
    }

    /**
     * 分页查询处置管理现场记录列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 处置管理现场记录分页列表
     */
    @Override
    public TableDataInfo<DzTaskHandleSceneRecordVo> queryPageList(DzTaskHandleSceneRecordBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DzTaskHandleSceneRecord> lqw = buildQueryWrapper(bo);
        Page<DzTaskHandleSceneRecordVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的处置管理现场记录列表
     *
     * @param bo 查询条件
     * @return 处置管理现场记录列表
     */
    @Override
    public List<DzTaskHandleSceneRecordVo> queryList(DzTaskHandleSceneRecordBo bo) {
        LambdaQueryWrapper<DzTaskHandleSceneRecord> lqw = buildQueryWrapper(bo);
        List<DzTaskHandleSceneRecordVo> list = baseMapper.selectVoList(lqw);
        if (list != null) {
            list.forEach(this::fillCompatibleFields);
        }
        return list;
    }

    private LambdaQueryWrapper<DzTaskHandleSceneRecord> buildQueryWrapper(DzTaskHandleSceneRecordBo bo) {
        LambdaQueryWrapper<DzTaskHandleSceneRecord> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(DzTaskHandleSceneRecord::getId);
        lqw.eq(bo.getHandleId() != null, DzTaskHandleSceneRecord::getHandleId, bo.getHandleId());
        lqw.eq(StringUtils.isNotBlank(bo.getLocation()), DzTaskHandleSceneRecord::getLocation, bo.getLocation());
        lqw.eq(StringUtils.isNotBlank(bo.getDisasterType()), DzTaskHandleSceneRecord::getDisasterType, bo.getDisasterType());
        lqw.like(StringUtils.isNotBlank(bo.getDisasterName()), DzTaskHandleSceneRecord::getDisasterName, bo.getDisasterName());
        lqw.eq(bo.getOccurrenceTime() != null, DzTaskHandleSceneRecord::getOccurrenceTime, bo.getOccurrenceTime());
        lqw.eq(StringUtils.isNotBlank(bo.getCauseType()), DzTaskHandleSceneRecord::getCauseType, bo.getCauseType());
        lqw.eq(StringUtils.isNotBlank(bo.getCauseRemark()), DzTaskHandleSceneRecord::getCauseRemark, bo.getCauseRemark());
        lqw.eq(bo.getThreatHouseholds() != null, DzTaskHandleSceneRecord::getThreatHouseholds, bo.getThreatHouseholds());
        lqw.eq(bo.getThreatPeople() != null, DzTaskHandleSceneRecord::getThreatPeople, bo.getThreatPeople());
        lqw.eq(bo.getThreatHouses() != null, DzTaskHandleSceneRecord::getThreatHouses, bo.getThreatHouses());
        lqw.eq(StringUtils.isNotBlank(bo.getOtherThreats()), DzTaskHandleSceneRecord::getOtherThreats, bo.getOtherThreats());
        lqw.eq(bo.getDirectLoss() != null, DzTaskHandleSceneRecord::getDirectLoss, bo.getDirectLoss());
        lqw.eq(bo.getCasualties() != null, DzTaskHandleSceneRecord::getCasualties, bo.getCasualties());
        lqw.eq(StringUtils.isNotBlank(bo.getInvestigationUnit()), DzTaskHandleSceneRecord::getInvestigationUnit, bo.getInvestigationUnit());
        lqw.eq(StringUtils.isNotBlank(bo.getInvestigator()), DzTaskHandleSceneRecord::getInvestigator, bo.getInvestigator());
        lqw.eq(bo.getInvestigationDate() != null, DzTaskHandleSceneRecord::getInvestigationDate, bo.getInvestigationDate());
        lqw.eq(StringUtils.isNotBlank(bo.getLithologyDesc()), DzTaskHandleSceneRecord::getLithologyDesc, bo.getLithologyDesc());
        lqw.eq(StringUtils.isNotBlank(bo.getJointFissureDesc()), DzTaskHandleSceneRecord::getJointFissureDesc, bo.getJointFissureDesc());
        lqw.eq(bo.getScaleLength() != null, DzTaskHandleSceneRecord::getScaleLength, bo.getScaleLength());
        lqw.eq(bo.getScaleWidth() != null, DzTaskHandleSceneRecord::getScaleWidth, bo.getScaleWidth());
        lqw.eq(bo.getScaleDepth() != null, DzTaskHandleSceneRecord::getScaleDepth, bo.getScaleDepth());
        lqw.eq(bo.getScaleVolume() != null, DzTaskHandleSceneRecord::getScaleVolume, bo.getScaleVolume());
        lqw.eq(StringUtils.isNotBlank(bo.getStabilityStatus()), DzTaskHandleSceneRecord::getStabilityStatus, bo.getStabilityStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getDevTrend()), DzTaskHandleSceneRecord::getDevTrend, bo.getDevTrend());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasuresEvacuation()), DzTaskHandleSceneRecord::getMeasuresEvacuation, bo.getMeasuresEvacuation());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasuresEvacuationRemark()), DzTaskHandleSceneRecord::getMeasuresEvacuationRemark, bo.getMeasuresEvacuationRemark());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasuresProtection()), DzTaskHandleSceneRecord::getMeasuresProtection, bo.getMeasuresProtection());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasuresProtectionRemark()), DzTaskHandleSceneRecord::getMeasuresProtectionRemark, bo.getMeasuresProtectionRemark());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasuresMonitoring()), DzTaskHandleSceneRecord::getMeasuresMonitoring, bo.getMeasuresMonitoring());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasuresMonitoringRemark()), DzTaskHandleSceneRecord::getMeasuresMonitoringRemark, bo.getMeasuresMonitoringRemark());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasuresHazardRemoval()), DzTaskHandleSceneRecord::getMeasuresHazardRemoval, bo.getMeasuresHazardRemoval());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasuresHazardRemovalRemark()), DzTaskHandleSceneRecord::getMeasuresHazardRemovalRemark, bo.getMeasuresHazardRemovalRemark());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasuresEngineering()), DzTaskHandleSceneRecord::getMeasuresEngineering, bo.getMeasuresEngineering());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasuresEngineeringRemark()), DzTaskHandleSceneRecord::getMeasuresEngineeringRemark, bo.getMeasuresEngineeringRemark());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasuresPublicity()), DzTaskHandleSceneRecord::getMeasuresPublicity, bo.getMeasuresPublicity());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasuresPublicityRemark()), DzTaskHandleSceneRecord::getMeasuresPublicityRemark, bo.getMeasuresPublicityRemark());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasuresTrafficControl()), DzTaskHandleSceneRecord::getMeasuresTrafficControl, bo.getMeasuresTrafficControl());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasuresTrafficControlRemark()), DzTaskHandleSceneRecord::getMeasuresTrafficControlRemark, bo.getMeasuresTrafficControlRemark());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasuresOtherSuggestions()), DzTaskHandleSceneRecord::getMeasuresOtherSuggestions, bo.getMeasuresOtherSuggestions());
        lqw.eq(StringUtils.isNotBlank(bo.getMeasuresOtherSuggestionsRemark()), DzTaskHandleSceneRecord::getMeasuresOtherSuggestionsRemark, bo.getMeasuresOtherSuggestionsRemark());
        lqw.eq(StringUtils.isNotBlank(bo.getPhotoPanorama()), DzTaskHandleSceneRecord::getPhotoPanorama, bo.getPhotoPanorama());
        lqw.eq(StringUtils.isNotBlank(bo.getPhotoDeformation()), DzTaskHandleSceneRecord::getPhotoDeformation, bo.getPhotoDeformation());
        lqw.eq(StringUtils.isNotBlank(bo.getPhotoDamage()), DzTaskHandleSceneRecord::getPhotoDamage, bo.getPhotoDamage());
        lqw.eq(bo.getCreateDate() != null, DzTaskHandleSceneRecord::getCreateDate, bo.getCreateDate());
        lqw.eq(bo.getUpdateDate() != null, DzTaskHandleSceneRecord::getUpdateDate, bo.getUpdateDate());
        lqw.eq(StringUtils.isNotBlank(bo.getDisasterCoordinates()), DzTaskHandleSceneRecord::getDisasterCoordinates, bo.getDisasterCoordinates());
        lqw.eq(StringUtils.isNotBlank(bo.getHazardExtent()), DzTaskHandleSceneRecord::getHazardExtent, bo.getHazardExtent());
        lqw.eq(StringUtils.isNotBlank(bo.getRiskExtent()), DzTaskHandleSceneRecord::getRiskExtent, bo.getRiskExtent());
        lqw.eq(StringUtils.isNotBlank(bo.getDisasterNature()), DzTaskHandleSceneRecord::getDisasterNature, bo.getDisasterNature());
        lqw.eq(bo.getThreatAsset() != null, DzTaskHandleSceneRecord::getThreatAsset, bo.getThreatAsset());
        lqw.eq(bo.getIndirectLoss() != null, DzTaskHandleSceneRecord::getIndirectLoss, bo.getIndirectLoss());
        lqw.eq(bo.getDeadPeople() != null, DzTaskHandleSceneRecord::getDeadPeople, bo.getDeadPeople());
        lqw.eq(StringUtils.isNotBlank(bo.getOtherDanger()), DzTaskHandleSceneRecord::getOtherDanger, bo.getOtherDanger());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeStructure()), DzTaskHandleSceneRecord::getSlopeStructure, bo.getSlopeStructure());
        lqw.eq(bo.getSlidingDirection() != null, DzTaskHandleSceneRecord::getSlidingDirection, bo.getSlidingDirection());
        return lqw;
    }

    /**
     * 新增处置管理现场记录
     *
     * @param bo 处置管理现场记录
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(DzTaskHandleSceneRecordBo bo) {
        normalizeAndValidateBo(bo);
        DzTaskHandleSceneRecord add = MapstructUtils.convert(bo, DzTaskHandleSceneRecord.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改处置管理现场记录
     *
     * @param bo 处置管理现场记录
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(DzTaskHandleSceneRecordBo bo) {
        if (bo == null) {
            throw new ServiceException("现场记录不能为空");
        }
        normalizeAndValidateBo(bo);
        DzTaskHandleSceneRecord update = MapstructUtils.convert(bo, DzTaskHandleSceneRecord.class);
        validEntityBeforeSave(update);
        update.setUpdateDate(new Date());
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(DzTaskHandleSceneRecord entity) {
        if (entity == null) {
            throw new ServiceException("现场记录不能为空");
        }
        if (entity.getHandleId() == null) {
            throw new ServiceException("handleId不能为空");
        }
        if (StringUtils.isBlank(entity.getDisasterType())) {
            throw new ServiceException("灾害类型不能为空");
        }
        if (StringUtils.isBlank(entity.getDisasterName())) {
            throw new ServiceException("灾害名称不能为空");
        }
        if (StringUtils.isBlank(entity.getLocation())) {
            throw new ServiceException("灾害地点不能为空");
        }
        if (StringUtils.isBlank(entity.getInvestigator())) {
            throw new ServiceException("调查人不能为空");
        }
        if (entity.getInvestigationDate() == null) {
            throw new ServiceException("调查时间不能为空");
        }
        if (StringUtils.isBlank(entity.getDisasterCoordinates())) {
            throw new ServiceException("灾害点坐标不能为空");
        }
        if (StringUtils.isBlank(entity.getHazardExtent())) {
            throw new ServiceException("危险范围不能为空");
        }
        if (StringUtils.isBlank(entity.getRiskExtent())) {
            throw new ServiceException("风险范围不能为空");
        }
        validateSceneRecordBusinessFields(entity);
        LambdaQueryWrapper<DzTaskHandleSceneRecord> lqw = Wrappers.<DzTaskHandleSceneRecord>lambdaQuery()
                                                                  .eq(DzTaskHandleSceneRecord::getHandleId, entity.getHandleId());
        if (entity.getId() != null) {
            lqw.ne(DzTaskHandleSceneRecord::getId, entity.getId());
        }
        if (baseMapper.selectCount(lqw) > 0) {
            throw new ServiceException("同一处置任务仅允许存在一条现场记录");
        }
    }

    /**
     * 校验并批量删除处置管理现场记录信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            List<DzTaskHandleSceneRecord> records = baseMapper.selectByIds(ids);
            if (records.size() != ids.size()) {
                throw new ServiceException("存在现场记录不存在，无法删除");
            }
            for (DzTaskHandleSceneRecord record : records) {
                DzTaskHandle handle = dzTaskHandleMapper.selectById(record.getHandleId());
                if (handle != null && handle.getHandleProcess() != null && handle.getHandleProcess() > 1) {
                    throw new ServiceException("已进入后续流程的处置任务现场记录不允许删除");
                }
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public DzTaskHandleSceneRecordVo getByHandleId(Long handleId) {
        LambdaQueryWrapper<DzTaskHandleSceneRecord> lqw = Wrappers.lambdaQuery();
        lqw.eq(DzTaskHandleSceneRecord::getHandleId, handleId);
        return fillCompatibleFields(baseMapper.selectVoOne(lqw));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void insertByReq(DzTaskHandleSceneRecordReq req) {
        if (req == null) {
            throw new ServiceException("现场记录不能为空");
        }
        String disasterPoint = CoordinateConverter.validatePointWkt(req.getDisasterCoordinates(), "灾害点坐标");
        req.setDisasterCoordinates(disasterPoint);
        String checkInPoint = CoordinateConverter.validatePointWkt(req.getCheckInCoordinates(), "打卡坐标");
        req.setCheckInCoordinates(checkInPoint);
        String hazardExtent = CoordinateConverter.validateGeometryWkt(req.getHazardExtent(), "危险范围");
        req.setHazardExtent(hazardExtent);
        String riskExtent = CoordinateConverter.validateGeometryWkt(req.getRiskExtent(), "风险范围");
        req.setRiskExtent(riskExtent);
        CoordinateConverter.validatePointWithinGeometry(disasterPoint, hazardExtent, "灾害点必须在危险范围内");
        CoordinateConverter.validateGeometryWithinGeometry(hazardExtent, riskExtent, "危险范围必须在风险范围内");
        Long handleId = req.getHandleId();
        DzTaskHandle dzTaskHandle = dzTaskHandleMapper.selectById(handleId);
        if (dzTaskHandle == null) {
            throw new ServiceException("处置管理不存在");
        }
        req.setPhotoPanorama(normalizePhotoPanorama(req.getPhotoPanorama()));
        DzTaskHandleSceneRecord add = MapstructUtils.convert(req, DzTaskHandleSceneRecord.class);
        add.setCreateDate(new Date());
        add.setUpdateDate(new Date());
        validEntityBeforeSave(add);
        if (baseMapper.insert(add) < 1) {
            throw new ServiceException("保存失败");
        }
        dzTaskHandle.setEventType(EventTypeEnum.getCodeByName(req.getDisasterType()));
        dzTaskHandle.setEventLevel(getLevelByDzTaskHandleSceneRecordReq(req));
        dzTaskHandle.setRespStatus(0);
        dzTaskHandle.setHandleProcess(1);
        dzTaskHandle.setInfluenceScope(req.getThreatHouseholds() + "户" + req.getThreatPeople() + "人");
        dzTaskHandle.setPeopleLeave(req.getThreatPeople() > 0 ? 1 : 0);
        dzTaskHandleMapper.updateById(dzTaskHandle);
        aiHostingOverviewNotifyService.notifyChangedAfterCommit(
            "task_handle_process",
            "scene_record_saved",
            "dz_task_handle",
            handleId,
            null
        );
    }

    private Integer getLevelByDzTaskHandleSceneRecordReq(DzTaskHandleSceneRecordReq dzTaskHandleSceneRecordReq) {
        return AiReportEventLevelSupport.calculateLevel(
            dzTaskHandleSceneRecordReq.getDeadPeople(),
            dzTaskHandleSceneRecordReq.getThreatPeople(),
            dzTaskHandleSceneRecordReq.getDirectLoss(),
            dzTaskHandleSceneRecordReq.getThreatAsset()
        );
    }

    static String normalizePhotoPanorama(String photoPanorama) {
        if (StringUtils.isBlank(photoPanorama)) {
            return photoPanorama;
        }
        return List.of(photoPanorama.split(","))
                   .stream()
                   .map(String::trim)
                   .filter(StringUtils::isNotBlank)
                   .map(DzTaskHandleSceneRecordServiceImpl::extractPhotoOssId)
                   .collect(Collectors.joining(","));
    }

    private static String extractPhotoOssId(String value) {
        int lastSlashIndex = value.lastIndexOf('/');
        if (lastSlashIndex < 0) {
            return value;
        }
        String ossId = value.substring(lastSlashIndex + 1).trim();
        return StringUtils.isBlank(ossId) ? value : ossId;
    }

    private void normalizeAndValidateBo(DzTaskHandleSceneRecordBo bo) {
        if (bo == null) {
            throw new ServiceException("现场记录不能为空");
        }
        String disasterPoint = CoordinateConverter.validatePointWkt(bo.getDisasterCoordinates(), "灾害点坐标");
        bo.setDisasterCoordinates(disasterPoint);
        if (StringUtils.isNotBlank(bo.getCheckInCoordinates())) {
            bo.setCheckInCoordinates(CoordinateConverter.validatePointWkt(bo.getCheckInCoordinates(), "打卡坐标"));
        }
        String hazardExtent = CoordinateConverter.validateGeometryWkt(bo.getHazardExtent(), "危险范围");
        bo.setHazardExtent(hazardExtent);
        String riskExtent = CoordinateConverter.validateGeometryWkt(bo.getRiskExtent(), "风险范围");
        bo.setRiskExtent(riskExtent);
        CoordinateConverter.validatePointWithinGeometry(disasterPoint, hazardExtent, "灾害点必须在危险范围内");
        CoordinateConverter.validateGeometryWithinGeometry(hazardExtent, riskExtent, "危险范围必须在风险范围内");
    }

    private void validateSceneRecordBusinessFields(DzTaskHandleSceneRecord entity) {
        if (StringUtils.isNotBlank(entity.getTakenMeasures()) && entity.getTakenMeasures().length() > TAKEN_MEASURES_MAX_LENGTH) {
            throw new ServiceException("已采取措施最多500个汉字");
        }
        validateJsonArrayText(entity.getPhotoDeformation(), "变形特征照片");
        validateJsonArrayText(entity.getPhotoDamage(), "受损情况照片");

        if (Objects.equals(entity.getDisasterType(), "滑坡")) {
            if (StringUtils.isBlank(entity.getDisasterNature())) {
                throw new ServiceException("滑体类型不能为空");
            }
            if (StringUtils.isBlank(entity.getLandsideMorphology())) {
                throw new ServiceException("滑坡形态不能为空");
            }
            if (entity.getSlidingDirection() == null) {
                throw new ServiceException("主滑方向不能为空");
            }
        }
        List<String> requiredFields = SPECIAL_REQUIRED_FIELDS.get(entity.getDisasterType());
        if (requiredFields == null) {
            throw new ServiceException("灾害类型仅支持滑坡、崩塌、地面塌陷、泥石流");
        }
        Map<String, Object> special = entity.getSpecialCharacteristics();
        for (String field : requiredFields) {
            Object value = special == null ? null : special.get(field);
            if (value == null || StringUtils.isBlank(String.valueOf(value))) {
                throw new ServiceException("专项字段[" + field + "]不能为空");
            }
        }
    }

    private void validateJsonArrayText(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        var node = JacksonUtil.toJsonNode(value);
        if (node == null || !node.isArray()) {
            throw new ServiceException(fieldName + "必须为JSON数组");
        }
        for (var item : node) {
            if (!item.isObject()
                || !item.hasNonNull("photoName")
                || !item.hasNonNull("photoDescription")
                || !item.hasNonNull("photo")
                || StringUtils.isBlank(item.get("photoName").asText())
                || StringUtils.isBlank(item.get("photoDescription").asText())
                || StringUtils.isBlank(item.get("photo").asText())) {
                throw new ServiceException("照片，名称，描述不得为空");
            }
        }
    }

    private DzTaskHandleSceneRecordVo fillCompatibleFields(DzTaskHandleSceneRecordVo vo) {
        if (vo == null) {
            return null;
        }
        vo.setWkt(vo.getRiskExtent());
        return vo;
    }
}
