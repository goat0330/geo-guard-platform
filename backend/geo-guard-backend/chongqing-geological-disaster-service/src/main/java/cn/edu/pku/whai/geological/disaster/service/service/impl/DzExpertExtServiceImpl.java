/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzExpertExt;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzExpertExtBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzExpertExtVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzExpertExtMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzExpertExtService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 专家扩展信息Service业务层处理
 *
 * @author kongweiguang
 * @date 2026-02-03
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DzExpertExtServiceImpl implements IDzExpertExtService {

    private final DzExpertExtMapper baseMapper;

    /**
     * 查询专家扩展信息
     *
     * @param id 主键
     * @return 专家扩展信息
     */
    @Override
    public DzExpertExtVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询专家扩展信息列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 专家扩展信息分页列表
     */
    @Override
    public TableDataInfo<DzExpertExtVo> queryPageList(DzExpertExtBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DzExpertExt> lqw = buildQueryWrapper(bo);
        Page<DzExpertExtVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的专家扩展信息列表
     *
     * @param bo 查询条件
     * @return 专家扩展信息列表
     */
    @Override
    public List<DzExpertExtVo> queryList(DzExpertExtBo bo) {
        LambdaQueryWrapper<DzExpertExt> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<DzExpertExt> buildQueryWrapper(DzExpertExtBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<DzExpertExt> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getUserId() != null, DzExpertExt::getUserId, bo.getUserId());
        lqw.eq(bo.getExpertType() != null, DzExpertExt::getExpertType, bo.getExpertType());
        lqw.eq(StringUtils.isNotBlank(bo.getIntroduction()), DzExpertExt::getIntroduction, bo.getIntroduction());
        lqw.eq(bo.getMeetingCount() != null, DzExpertExt::getMeetingCount, bo.getMeetingCount());
        return lqw;
    }

    /**
     * 新增专家扩展信息
     *
     * @param bo 专家扩展信息
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(DzExpertExtBo bo) {
        DzExpertExt add = MapstructUtils.convert(bo, DzExpertExt.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改专家扩展信息
     *
     * @param bo 专家扩展信息
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(DzExpertExtBo bo) {
        DzExpertExt update = MapstructUtils.convert(bo, DzExpertExt.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(DzExpertExt entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除专家扩展信息信息
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
