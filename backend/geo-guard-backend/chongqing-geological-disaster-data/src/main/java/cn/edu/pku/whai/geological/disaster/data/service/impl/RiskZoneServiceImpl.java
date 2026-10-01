/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;


import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RiskZoneBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.RiskZone;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RiskZoneStatVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RiskZoneVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.RiskZoneMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IRiskZoneService;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 风险区Service业务层处理
 *
 * @author lizheng
 * @date 2026-01-10
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RiskZoneServiceImpl implements IRiskZoneService {

    private final RiskZoneMapper baseMapper;

    /**
     * 查询风险区
     *
     * @param id 主键
     * @return 风险区
     */
    @Override
    public RiskZoneVo queryById(String id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询风险区列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 风险区分页列表
     */
    @Override
    public TableDataInfo<RiskZoneVo> queryPageList(RiskZoneBo bo, PageQuery pageQuery) {
        QueryWrapper<RiskZone> qew = buildQueryWrapper(bo);
        Page<RiskZoneVo> result;
        result = baseMapper.selectVoPage(pageQuery.build(), qew);
        return TableDataInfo.build(result);
    }


    private QueryWrapper<RiskZone> buildQueryWrapper(RiskZoneBo bo) {
        Map<String, Object> params = bo.getParams();
        QueryWrapper<RiskZone> qew = Wrappers.query();
        qew.likeRight(ObjUtil.isNotNull(bo.getId()), "dsu.id", bo.getId());

        qew.eq(bo.getPilotArea1() != null, "pilot_area_1", bo.getPilotArea1());
        qew.eq(bo.getPilotArea2() != null, "pilot_area_2", bo.getPilotArea2());
        qew.eq(StringUtils.isNotBlank(bo.getProvince()), "province", bo.getProvince());
        qew.eq(StringUtils.isNotBlank(bo.getCity()), "city", bo.getCity());
        qew.eq(StringUtils.isNotBlank(bo.getCounty()), "county", bo.getCounty());
        qew.eq(StringUtils.isNotBlank(bo.getStreet()), "street", bo.getStreet());
        qew.eq(StringUtils.isNotBlank(bo.getVillage()), "village", bo.getVillage());
        qew.eq(StringUtils.isNotBlank(bo.getCommunity()), "community", bo.getCommunity());
        qew.eq(StringUtils.isNotBlank(bo.getCenter()), "center", bo.getCenter());
        qew.eq(StringUtils.isNotBlank(bo.getWkt()), "wkt", bo.getWkt());
        qew.eq(bo.getArea() != null, "area", bo.getArea());
        qew.eq(StringUtils.isNotBlank(bo.getProvinceCode()), "province_code", bo.getProvinceCode());
        qew.eq(StringUtils.isNotBlank(bo.getCityCode()), "city_code", bo.getCityCode());
        qew.eq(StringUtils.isNotBlank(bo.getCountyCode()), "county_code", bo.getCountyCode());
        qew.eq(StringUtils.isNotBlank(bo.getStreetCode()), "street_code", bo.getStreetCode());
        qew.eq(StringUtils.isNotBlank(bo.getVillageCode()), "village_code", bo.getVillageCode());
        if (bo.getDynamicRiskLevelList() != null) {
            qew.in("dra.dynamic_risk_level", bo.getDynamicRiskLevelList());
            qew.ge("dra.create_date", DateUtil.beginOfDay(new Date()));

        }
        qew.orderByAsc("id");
        return qew;
    }

    @Override
    public List<RiskZoneVo> queryByArea(String area) {
        QueryWrapper<RiskZone> lqw = Wrappers.query();
        lqw.like("concat(province, city, county, street, village, community)", area);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public List<RiskZoneStatVo> statRiskLevel() {
        return baseMapper.statRiskLevel();
    }
}
