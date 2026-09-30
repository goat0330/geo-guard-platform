/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzMsgNoticeBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzMsgNotice;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzMsgNoticeVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.MsgNoticeCountStat;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzMsgNoticeMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzMsgNoticeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 业务消息通知Service业务层处理
 *
 * @author kongweiguang
 * @date 2026-04-07
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DzMsgNoticeServiceImpl implements IDzMsgNoticeService {

    private final DzMsgNoticeMapper baseMapper;

    /**
     * 查询业务消息通知
     *
     * @param id 主键
     * @return 业务消息通知
     */
    @Override
    public DzMsgNoticeVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询业务消息通知列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 业务消息通知分页列表
     */
    @Override
    public TableDataInfo<DzMsgNoticeVo> queryPageList(DzMsgNoticeBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DzMsgNotice> lqw = buildQueryWrapper(bo);
        Page<DzMsgNoticeVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的业务消息通知列表
     *
     * @param bo 查询条件
     * @return 业务消息通知列表
     */
    @Override
    public List<DzMsgNoticeVo> queryList(DzMsgNoticeBo bo) {
        LambdaQueryWrapper<DzMsgNotice> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<DzMsgNotice> buildQueryWrapper(DzMsgNoticeBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<DzMsgNotice> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTitle()), DzMsgNotice::getTitle, bo.getTitle());
        lqw.eq(StringUtils.isNotBlank(bo.getContent()), DzMsgNotice::getContent, bo.getContent());
        lqw.eq(StringUtils.isNotBlank(bo.getType()), DzMsgNotice::getType, bo.getType());
        lqw.eq(bo.getStatus() != null, DzMsgNotice::getStatus, bo.getStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getBizData()), DzMsgNotice::getBizData, bo.getBizData());

        if (params.get("beginTime") != null && params.get("endTime") != null) {
            lqw.between(DzMsgNotice::getCreateDate, params.get("beginTime"), params.get("endTime"));
        }

        lqw.eq(bo.getUpdateDate() != null, DzMsgNotice::getUpdateDate, bo.getUpdateDate());
        lqw.eq(bo.getUserId() != null, DzMsgNotice::getUserId, bo.getUserId());
        return lqw;
    }

    /**
     * 新增业务消息通知
     *
     * @param bo 业务消息通知
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(DzMsgNoticeBo bo) {
        DzMsgNotice add = MapstructUtils.convert(bo, DzMsgNotice.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改业务消息通知
     *
     * @param bo 业务消息通知
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(DzMsgNoticeBo bo) {
        DzMsgNotice update = MapstructUtils.convert(bo, DzMsgNotice.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(DzMsgNotice entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除业务消息通知信息
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

    @Override
    public void handle(DzMsgNoticeBo bo) {
        if (bo.getIds() == null || bo.getIds().isEmpty()) {
            throw new IllegalArgumentException("请选择要处理的业务消息通知");
        }
        for (Long id : bo.getIds()) {
            bo.setId(id);
            bo.setUpdateDate(new Date());
            bo.setStatus(1);
            updateByBo(bo);
        }
    }

    @Override
    public MsgNoticeCountStat countStatus(Long userId) {
        MsgNoticeCountStat stat = baseMapper.countStatus(userId);
        return stat;
    }
}
