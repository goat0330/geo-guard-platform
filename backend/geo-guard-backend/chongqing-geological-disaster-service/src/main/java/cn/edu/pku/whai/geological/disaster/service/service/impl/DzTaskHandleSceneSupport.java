/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.po.EngineeringGeology;
import cn.edu.pku.whai.geological.disaster.data.domain.po.Stratum;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.GeologyInfoVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.SlopeUnitGridMemberRelationVo;
import cn.edu.pku.whai.geological.disaster.data.service.IGeologyInfoService;
import cn.edu.pku.whai.geological.disaster.data.service.ISlopeUnitGridMemberRelationService;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.DzTaskHandleSceneRecordReq;
import cn.edu.pku.whai.geological.disaster.service.app.utils.GeoDistanceUtil;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandleSceneRecord;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleSceneRecordMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class DzTaskHandleSceneSupport {

    private final DzTaskHandleSceneRecordMapper dzTaskHandleSceneRecordMapper;
    private final ISlopeUnitGridMemberRelationService slopeUnitGridMemberRelationService;
    private final IGeologyInfoService geologyInfoService;

    void fillSceneRecordFields(DzTaskHandleVo vo) {
        if (vo == null || vo.getId() == null) {
            return;
        }
        Map<Long, DzTaskHandleSceneRecord> sceneRecordByHandleId = buildSceneRecordByHandleId(List.of(vo.getId()));
        DzTaskHandleSceneRecord sceneRecord = sceneRecordByHandleId.get(vo.getId());
        if (sceneRecord == null) {
            return;
        }
        vo.setHazardExtent(sceneRecord.getHazardExtent());
        vo.setRiskExtent(sceneRecord.getRiskExtent());
    }

    void fillSceneRecordFields(List<DzTaskHandleVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> handleIds = list.stream()
                                   .filter(Objects::nonNull)
                                   .map(DzTaskHandleVo::getId)
                                   .filter(Objects::nonNull)
                                   .toList();
        if (handleIds.isEmpty()) {
            return;
        }
        Map<Long, DzTaskHandleSceneRecord> sceneRecordByHandleId = buildSceneRecordByHandleId(handleIds);
        for (DzTaskHandleVo vo : list) {
            if (vo == null || vo.getId() == null) {
                continue;
            }
            DzTaskHandleSceneRecord sceneRecord = sceneRecordByHandleId.get(vo.getId());
            if (sceneRecord == null) {
                continue;
            }
            vo.setHazardExtent(sceneRecord.getHazardExtent());
            vo.setRiskExtent(sceneRecord.getRiskExtent());
        }
    }

    void fillRelationFields(DzTaskHandleVo vo) {
        if (vo == null || StringUtils.isBlank(vo.getSlopeUnitId())) {
            return;
        }
        SlopeUnitGridMemberRelationVo relationVo =
            slopeUnitGridMemberRelationService.queryByUnitId(vo.getSlopeUnitId().trim());
        if (relationVo == null) {
            return;
        }
        vo.setVillageSecretary(relationVo.getSpecialManager());
        vo.setVillageSecretaryPhone(relationVo.getSpecialManagerPhone());
    }

    void fillRelationFields(List<DzTaskHandleVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (DzTaskHandleVo vo : list) {
            fillRelationFields(vo);
        }
    }

    void fillGeologyInfo(DzTaskHandleSceneRecordReq req, String point) {
        if (req == null) {
            throw new ServiceException("现场记录不能为空");
        }
        if (StringUtils.isNotBlank(req.getLithologyDesc()) && StringUtils.isNotBlank(req.getJointFissureDesc())) {
            return;
        }
        double[] coordinates = GeoDistanceUtil.parsePoint(point);
        if (coordinates == null || coordinates.length < 2) {
            return;
        }
        GeologyInfoVo geologyInfoVo = geologyInfoService.queryByPoint(coordinates[0], coordinates[1]);
        if (geologyInfoVo == null) {
            return;
        }
        if (StringUtils.isBlank(req.getLithologyDesc())) {
            req.setLithologyDesc(buildLithologyDesc(geologyInfoVo));
        }
        if (StringUtils.isBlank(req.getJointFissureDesc())) {
            req.setJointFissureDesc(buildJointFissureDesc(geologyInfoVo));
        }
    }

    private Map<Long, DzTaskHandleSceneRecord> buildSceneRecordByHandleId(List<Long> handleIds) {
        if (handleIds == null || handleIds.isEmpty()) {
            return Map.of();
        }
        return dzTaskHandleSceneRecordMapper.selectList(
            Wrappers.<DzTaskHandleSceneRecord>lambdaQuery()
                .select(
                    DzTaskHandleSceneRecord::getHandleId,
                    DzTaskHandleSceneRecord::getHazardExtent,
                    DzTaskHandleSceneRecord::getRiskExtent
                )
                .in(DzTaskHandleSceneRecord::getHandleId, handleIds)
        ).stream()
         .filter(Objects::nonNull)
         .filter(record -> record.getHandleId() != null)
         .collect(Collectors.toMap(
             DzTaskHandleSceneRecord::getHandleId,
             record -> record,
             (left, right) -> left
         ));
    }

    private String buildLithologyDesc(GeologyInfoVo geologyInfoVo) {
        if (geologyInfoVo == null || geologyInfoVo.getStratums() == null) {
            return null;
        }
        for (Stratum stratum : geologyInfoVo.getStratums()) {
            if (StringUtils.isNotBlank(stratum.getLithology())) {
                return stratum.getLithology();
            }
            if (StringUtils.isNotBlank(stratum.getDescription())) {
                return stratum.getDescription();
            }
        }
        return null;
    }

    private String buildJointFissureDesc(GeologyInfoVo geologyInfoVo) {
        if (geologyInfoVo == null || geologyInfoVo.getEngineeringGeologies() == null) {
            return null;
        }
        for (EngineeringGeology engineeringGeology : geologyInfoVo.getEngineeringGeologies()) {
            if (StringUtils.isNotBlank(engineeringGeology.getCharacteristics())) {
                return engineeringGeology.getCharacteristics();
            }
            if (StringUtils.isNotBlank(engineeringGeology.getRockGroup())) {
                return engineeringGeology.getRockGroup();
            }
            if (StringUtils.isNotBlank(engineeringGeology.getProjectName())) {
                return engineeringGeology.getProjectName();
            }
        }
        return null;
    }
}
