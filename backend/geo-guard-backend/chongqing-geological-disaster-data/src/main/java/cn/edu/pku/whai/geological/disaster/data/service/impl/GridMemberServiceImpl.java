/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl;


import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.GridMemberBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.GridMember;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.GridMemberVo;
import cn.edu.pku.whai.geological.disaster.data.mapper.GridMemberMapper;
import cn.edu.pku.whai.geological.disaster.data.service.IGridMemberService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 网格人员信息Service业务层处理
 **/
@RequiredArgsConstructor
@Service
public class GridMemberServiceImpl implements IGridMemberService {

    private final GridMemberMapper baseMapper;

    /**
     * 查询网格人员信息
     *
     * @param userId 主键
     * @return 网格人员信息
     */
    @Override
    public GridMemberVo queryById(String userId) {
        return baseMapper.selectVoById(userId);
    }

    /**
     * 分页查询网格人员信息列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 网格人员信息分页列表
     */
    @Override
    public TableDataInfo<GridMemberVo> queryPageList(GridMemberBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<GridMember> lqw = buildQueryWrapper(bo);
        Page<GridMemberVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的网格人员信息列表
     *
     * @param bo 查询条件
     * @return 网格人员信息列表
     */
    @Override
    public List<GridMemberVo> queryList(GridMemberBo bo) {
        LambdaQueryWrapper<GridMember> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<GridMember> buildQueryWrapper(GridMemberBo bo) {
        LambdaQueryWrapper<GridMember> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getPhonenumber()), GridMember::getPhonenumber, bo.getPhonenumber());
        lqw.eq(StringUtils.isNotBlank(bo.getGridCode()), GridMember::getGridCode, bo.getGridCode());
        return lqw;
    }

    /**
     * 新增网格人员信息
     *
     * @param bo 网格人员信息
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(GridMemberBo bo) {
        GridMember add = MapstructUtils.convert(bo, GridMember.class);
        validEntityBeforeSave(add);
        return baseMapper.insert(add) > 0;
    }

    /**
     * 修改网格人员信息
     *
     * @param bo 网格人员信息
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(GridMemberBo bo) {
        GridMember update = MapstructUtils.convert(bo, GridMember.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(GridMember entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除网格人员信息信息
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


    @Override
    public GridMember queryByGridId(String village) {
        List<GridMember> gridMembers = baseMapper.selectList(Wrappers.lambdaQuery(GridMember.class).eq(GridMember::getGridCode, village));
        return gridMembers.isEmpty() ? null : gridMembers.get(0);
    }
}
