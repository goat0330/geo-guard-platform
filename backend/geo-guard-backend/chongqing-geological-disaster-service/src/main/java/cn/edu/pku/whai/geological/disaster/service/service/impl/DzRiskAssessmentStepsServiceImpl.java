/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;


import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskAssessmentStepsBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessmentSteps;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskAssessmentStepsVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentStepsMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzRiskAssessmentStepsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 地质灾害预测与易发性评价数据Service业务层处理
 *
 * @author kongweiguang
 * @date 2026-01-19
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DzRiskAssessmentStepsServiceImpl implements IDzRiskAssessmentStepsService {

    private final DzRiskAssessmentStepsMapper baseMapper;

    /**
     * 查询地质灾害预测与易发性评价数据
     *
     * @param id 主键
     * @return 地质灾害预测与易发性评价数据
     */
    @Override
    public DzRiskAssessmentStepsVo queryById(Long id) {
        return MapstructUtils.convert(baseMapper.selectById(id), DzRiskAssessmentStepsVo.class);
    }

    /**
     * 分页查询地质灾害预测与易发性评价数据列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 地质灾害预测与易发性评价数据分页列表
     */
    @Override
    public TableDataInfo<DzRiskAssessmentStepsVo> queryPageList(DzRiskAssessmentStepsBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DzRiskAssessmentSteps> lqw = buildQueryWrapper(bo);
        Page<DzRiskAssessmentSteps> result = baseMapper.selectPage(pageQuery.build(), lqw);
        Page<DzRiskAssessmentStepsVo> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(MapstructUtils.convert(result.getRecords(), DzRiskAssessmentStepsVo.class));
        return TableDataInfo.build(voPage);
    }

    /**
     * 查询符合条件的地质灾害预测与易发性评价数据列表
     *
     * @param bo 查询条件
     * @return 地质灾害预测与易发性评价数据列表
     */
    @Override
    public List<DzRiskAssessmentStepsVo> queryList(DzRiskAssessmentStepsBo bo) {
        LambdaQueryWrapper<DzRiskAssessmentSteps> lqw = buildQueryWrapper(bo);
        return MapstructUtils.convert(baseMapper.selectList(lqw), DzRiskAssessmentStepsVo.class);
    }

    private LambdaQueryWrapper<DzRiskAssessmentSteps> buildQueryWrapper(DzRiskAssessmentStepsBo bo) {
        List<Integer> disasterPoints = normalizeIntegerList(bo.getDisasterPoints());
        LambdaQueryWrapper<DzRiskAssessmentSteps> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(DzRiskAssessmentSteps::getId);
        lqw.eq(StringUtils.isNotBlank(bo.getOriginalObjectId()), DzRiskAssessmentSteps::getOriginalObjectId, bo.getOriginalObjectId());
        lqw.eq(bo.getGridCode() != null, DzRiskAssessmentSteps::getGridCode, bo.getGridCode());
        lqw.eq(bo.getAreaSize() != null, DzRiskAssessmentSteps::getAreaSize, bo.getAreaSize());
        lqw.eq(bo.getShapeLength() != null, DzRiskAssessmentSteps::getShapeLength, bo.getShapeLength());
        lqw.eq(StringUtils.isNotBlank(bo.getDescription()), DzRiskAssessmentSteps::getDescription, bo.getDescription());
        lqw.eq(bo.getMpArea() != null, DzRiskAssessmentSteps::getMpArea, bo.getMpArea());
        lqw.eq(bo.getMpPerimeter() != null, DzRiskAssessmentSteps::getMpPerimeter, bo.getMpPerimeter());
        lqw.eq(bo.getPointNo() != null, DzRiskAssessmentSteps::getPointNo, bo.getPointNo());
        lqw.eq(StringUtils.isNotBlank(bo.getStability()), DzRiskAssessmentSteps::getStability, bo.getStability());
        lqw.eq(StringUtils.isNotBlank(bo.getStructureTypeSub()), DzRiskAssessmentSteps::getStructureTypeSub, bo.getStructureTypeSub());
        lqw.like(StringUtils.isNotBlank(bo.getSlopeName()), DzRiskAssessmentSteps::getSlopeName, bo.getSlopeName());
        lqw.eq(bo.getShapeLength1() != null, DzRiskAssessmentSteps::getShapeLength1, bo.getShapeLength1());
        lqw.eq(bo.getStructureVal() != null, DzRiskAssessmentSteps::getStructureVal, bo.getStructureVal());
        lqw.eq(bo.getLandslideProne() != null, DzRiskAssessmentSteps::getLandslideProne, bo.getLandslideProne());
        lqw.eq(bo.getLandslidePronePy() != null, DzRiskAssessmentSteps::getLandslidePronePy, bo.getLandslidePronePy());
        lqw.eq(bo.getShapeLength2() != null, DzRiskAssessmentSteps::getShapeLength2, bo.getShapeLength2());
        lqw.eq(bo.getShapeArea() != null, DzRiskAssessmentSteps::getShapeArea, bo.getShapeArea());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeStructure()), DzRiskAssessmentSteps::getSlopeStructure, bo.getSlopeStructure());
        lqw.eq(bo.getElevationMean() != null, DzRiskAssessmentSteps::getElevationMean, bo.getElevationMean());
        lqw.eq(bo.getElevationMax() != null, DzRiskAssessmentSteps::getElevationMax, bo.getElevationMax());
        lqw.eq(bo.getElevationMin() != null, DzRiskAssessmentSteps::getElevationMin, bo.getElevationMin());
        lqw.eq(bo.getElevationDiff() != null, DzRiskAssessmentSteps::getElevationDiff, bo.getElevationDiff());
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeMean()), DzRiskAssessmentSteps::getSlopeMean, bo.getSlopeMean());
        lqw.eq(bo.getAspectMean() != null, DzRiskAssessmentSteps::getAspectMean, bo.getAspectMean());
        lqw.eq(bo.getPlanCurvature() != null, DzRiskAssessmentSteps::getPlanCurvature, bo.getPlanCurvature());
        lqw.eq(bo.getProfileCurvature() != null, DzRiskAssessmentSteps::getProfileCurvature, bo.getProfileCurvature());
        lqw.eq(StringUtils.isNotBlank(bo.getLithologyDesc()), DzRiskAssessmentSteps::getLithologyDesc, bo.getLithologyDesc());
        lqw.eq(bo.getSusceptibilityVal() != null, DzRiskAssessmentSteps::getSusceptibilityVal, bo.getSusceptibilityVal());
        applyDisasterPointsCondition(lqw, disasterPoints);
        lqw.eq(StringUtils.isNotBlank(bo.getSlopeMorphology()), DzRiskAssessmentSteps::getSlopeMorphology, bo.getSlopeMorphology());
        lqw.eq(StringUtils.isNotBlank(bo.getUnitMorphology()), DzRiskAssessmentSteps::getUnitMorphology, bo.getUnitMorphology());
        lqw.eq(StringUtils.isNotBlank(bo.getVegetationCover()), DzRiskAssessmentSteps::getVegetationCover, bo.getVegetationCover());
        lqw.eq(StringUtils.isNotBlank(bo.getAdjacentWater()), DzRiskAssessmentSteps::getAdjacentWater, bo.getAdjacentWater());
        lqw.eq(StringUtils.isNotBlank(bo.getTectonicDist()), DzRiskAssessmentSteps::getTectonicDist, bo.getTectonicDist());
        lqw.eq(bo.getSlopeStructureCode() != null, DzRiskAssessmentSteps::getSlopeStructureCode, bo.getSlopeStructureCode());
        lqw.eq(bo.getShuomingCode() != null, DzRiskAssessmentSteps::getShuomingCode, bo.getShuomingCode());
        lqw.eq(bo.getSusceptibilityCode() != null, DzRiskAssessmentSteps::getSusceptibilityCode, bo.getSusceptibilityCode());
        lqw.eq(bo.getSlopeMorphCode() != null, DzRiskAssessmentSteps::getSlopeMorphCode, bo.getSlopeMorphCode());
        lqw.eq(bo.getUnitMorphCode() != null, DzRiskAssessmentSteps::getUnitMorphCode, bo.getUnitMorphCode());
        lqw.eq(bo.getVegetationCoverCode() != null, DzRiskAssessmentSteps::getVegetationCoverCode, bo.getVegetationCoverCode());
        lqw.eq(bo.getAdjacentToWaterCode() != null, DzRiskAssessmentSteps::getAdjacentToWaterCode, bo.getAdjacentToWaterCode());
        lqw.eq(bo.getStructureCode() != null, DzRiskAssessmentSteps::getStructureCode, bo.getStructureCode());
        lqw.eq(bo.getAssessmentId() != null, DzRiskAssessmentSteps::getAssessmentId, bo.getAssessmentId());
        lqw.eq(bo.getPopCount() != null, DzRiskAssessmentSteps::getPopCount, bo.getPopCount());
        lqw.eq(bo.getEconSum() != null, DzRiskAssessmentSteps::getEconSum, bo.getEconSum());
        lqw.eq(bo.getRoadDensity() != null, DzRiskAssessmentSteps::getRoadDensity, bo.getRoadDensity());
        lqw.eq(bo.getBuildingDensity() != null, DzRiskAssessmentSteps::getBuildingDensity, bo.getBuildingDensity());
        lqw.eq(bo.getPopDensity() != null, DzRiskAssessmentSteps::getPopDensity, bo.getPopDensity());
        lqw.eq(bo.getRainfallProb() != null, DzRiskAssessmentSteps::getRainfallProb, bo.getRainfallProb());
        lqw.eq(bo.getRainfallPast7() != null, DzRiskAssessmentSteps::getRainfallPast7, bo.getRainfallPast7());
        lqw.eq(bo.getRainfallForecast() != null, DzRiskAssessmentSteps::getRainfallForecast, bo.getRainfallForecast());
        lqw.eq(bo.getCombinedTimeProb() != null, DzRiskAssessmentSteps::getCombinedTimeProb, bo.getCombinedTimeProb());
        return lqw;
    }

    private void applyDisasterPointsCondition(LambdaQueryWrapper<DzRiskAssessmentSteps> lqw, List<Integer> disasterPoints) {
        if (disasterPoints.isEmpty()) {
            return;
        }
        lqw.apply("disaster_points = CAST({0} AS integer[])", toPgIntArrayLiteral(disasterPoints));
    }

    private List<Integer> normalizeIntegerList(List<Integer> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private String toPgIntArrayLiteral(List<Integer> values) {
        return values.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "{", "}"));
    }

    /**
     * 新增地质灾害预测与易发性评价数据
     *
     * @param bo 地质灾害预测与易发性评价数据
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(DzRiskAssessmentStepsBo bo) {
        DzRiskAssessmentSteps add = MapstructUtils.convert(bo, DzRiskAssessmentSteps.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改地质灾害预测与易发性评价数据
     *
     * @param bo 地质灾害预测与易发性评价数据
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(DzRiskAssessmentStepsBo bo) {
        DzRiskAssessmentSteps update = MapstructUtils.convert(bo, DzRiskAssessmentSteps.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(DzRiskAssessmentSteps entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除地质灾害预测与易发性评价数据信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteByIds(ids) > 0;
    }
}
