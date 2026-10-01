/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;


import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.OrganizationInfoBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.OrganizationInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.OrganizationInfoVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.OrganizationInfoMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IOrganizationInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 组织机构信息Service业务层处理
 **/
@RequiredArgsConstructor
@Service
public class OrganizationInfoServiceImpl implements IOrganizationInfoService {

    private final OrganizationInfoMapper baseMapper;

    /**
     * 查询组织机构信息
     *
     * @param id 主键
     * @return 组织机构信息
     */
    @Override
    public OrganizationInfoVo queryById(String id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询组织机构信息列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 组织机构信息分页列表
     */
    @Override
    public TableDataInfo<OrganizationInfoVo> queryPageList(OrganizationInfoBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<OrganizationInfo> lqw = buildQueryWrapper(bo);
        Page<OrganizationInfoVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的组织机构信息列表
     *
     * @param bo 查询条件
     * @return 组织机构信息列表
     */
    @Override
    public List<OrganizationInfoVo> queryList(OrganizationInfoBo bo) {
        LambdaQueryWrapper<OrganizationInfo> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<OrganizationInfo> buildQueryWrapper(OrganizationInfoBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<OrganizationInfo> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getOrganizationFullName()), OrganizationInfo::getOrganizationFullName, bo.getOrganizationFullName());
        lqw.eq(StringUtils.isNotBlank(bo.getParentOrganizationId()), OrganizationInfo::getParentOrganizationId, bo.getParentOrganizationId());
        lqw.like(StringUtils.isNotBlank(bo.getOrganizationShortName()), OrganizationInfo::getOrganizationShortName, bo.getOrganizationShortName());
        lqw.eq(StringUtils.isNotBlank(bo.getAdministrativeRegionId()), OrganizationInfo::getAdministrativeRegionId, bo.getAdministrativeRegionId());
        lqw.eq(StringUtils.isNotBlank(bo.getOrganizationCode()), OrganizationInfo::getOrganizationCode, bo.getOrganizationCode());
        lqw.eq(bo.getSortNumber() != null, OrganizationInfo::getSortNumber, bo.getSortNumber());
        lqw.eq(bo.getOrganizationLevel() != null, OrganizationInfo::getOrganizationLevel, bo.getOrganizationLevel());
        lqw.eq(StringUtils.isNotBlank(bo.getContactPhone()), OrganizationInfo::getContactPhone, bo.getContactPhone());
        lqw.eq(StringUtils.isNotBlank(bo.getContactPerson()), OrganizationInfo::getContactPerson, bo.getContactPerson());
        lqw.eq(StringUtils.isNotBlank(bo.getEmailAddress()), OrganizationInfo::getEmailAddress, bo.getEmailAddress());
        lqw.eq(StringUtils.isNotBlank(bo.getDetailedAddress()), OrganizationInfo::getDetailedAddress, bo.getDetailedAddress());
        lqw.eq(StringUtils.isNotBlank(bo.getLogoImagePath()), OrganizationInfo::getLogoImagePath, bo.getLogoImagePath());
        lqw.eq(StringUtils.isNotBlank(bo.getFaxNumber()), OrganizationInfo::getFaxNumber, bo.getFaxNumber());
        lqw.eq(StringUtils.isNotBlank(bo.getCreatedBy()), OrganizationInfo::getCreatedBy, bo.getCreatedBy());
        lqw.eq(bo.getCreatedTime() != null, OrganizationInfo::getCreatedTime, bo.getCreatedTime());
        lqw.eq(StringUtils.isNotBlank(bo.getUpdatedBy()), OrganizationInfo::getUpdatedBy, bo.getUpdatedBy());
        lqw.eq(bo.getUpdatedTime() != null, OrganizationInfo::getUpdatedTime, bo.getUpdatedTime());
        lqw.eq(StringUtils.isNotBlank(bo.getExternalOrganizationId()), OrganizationInfo::getExternalOrganizationId, bo.getExternalOrganizationId());
        return lqw;
    }

    /**
     * 新增组织机构信息
     *
     * @param bo 组织机构信息
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(OrganizationInfoBo bo) {
        OrganizationInfo add = MapstructUtils.convert(bo, OrganizationInfo.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改组织机构信息
     *
     * @param bo 组织机构信息
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(OrganizationInfoBo bo) {
        OrganizationInfo update = MapstructUtils.convert(bo, OrganizationInfo.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(OrganizationInfo entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除组织机构信息信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (isValid) {
            // TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteByIds(ids) > 0;
    }
}
