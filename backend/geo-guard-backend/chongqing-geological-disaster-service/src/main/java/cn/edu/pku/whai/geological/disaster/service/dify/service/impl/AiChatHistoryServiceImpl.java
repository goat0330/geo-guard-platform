package cn.edu.pku.whai.geological.disaster.service.dify.service.impl;


import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.AiChatHistoryBo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.AiChatHistory;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.AiChatHistoryDetail;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.vo.AiChatHistoryDetailVo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.vo.AiChatHistoryVo;
import cn.edu.pku.whai.geological.disaster.service.dify.mapper.AiChatHistoryDetailMapper;
import cn.edu.pku.whai.geological.disaster.service.dify.mapper.AiChatHistoryMapper;
import cn.edu.pku.whai.geological.disaster.service.dify.service.IAiChatHistoryService;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.util.ObjUtil;
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
 * 对话历史Service业务层处理
 *
 * @author kongweiguang
 * @date 2026-01-10
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AiChatHistoryServiceImpl implements IAiChatHistoryService {

    private final AiChatHistoryMapper baseMapper;
    private final AiChatHistoryDetailMapper aiChatHistoryDetailMapper;

    /**
     * 查询对话历史
     *
     * @param id 主键
     * @return 对话历史
     */
    @Override
    public AiChatHistoryVo queryById(String id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询对话历史列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 对话历史分页列表
     */
    @Override
    public TableDataInfo<AiChatHistoryVo> queryPageList(AiChatHistoryBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<AiChatHistory> lqw = buildQueryWrapper(bo);
        Page<AiChatHistoryVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        List<AiChatHistoryVo> records = result.getRecords();
        if (ObjUtil.isEmpty(records)) {
            return TableDataInfo.build(result);
        }

        List<String> list = records.stream().map(AiChatHistoryVo::getId).toList();
        List<AiChatHistoryDetailVo> aiChatHistoryDetailVos = aiChatHistoryDetailMapper.selectVoLastByHistoryIds(Wrappers.<AiChatHistoryDetail>lambdaQuery()
                .in(AiChatHistoryDetail::getHistoryId, list));
        Map<String, AiChatHistoryDetailVo> map = CollStreamUtil.toIdentityMap(aiChatHistoryDetailVos, AiChatHistoryDetailVo::getHistoryId);
        records.forEach(record -> record.setLastDetail(map.get(record.getId())));
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的对话历史列表
     *
     * @param bo 查询条件
     * @return 对话历史列表
     */
    @Override
    public List<AiChatHistoryVo> queryList(AiChatHistoryBo bo) {
        LambdaQueryWrapper<AiChatHistory> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<AiChatHistory> buildQueryWrapper(AiChatHistoryBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<AiChatHistory> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getUserId() != null, AiChatHistory::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getType()), AiChatHistory::getType, bo.getType());
        lqw.like(StringUtils.isNotBlank(bo.getTitle()), AiChatHistory::getTitle, bo.getTitle());
        lqw.eq(bo.getCreateDate() != null, AiChatHistory::getCreateDate, bo.getCreateDate());
        lqw.eq(bo.getUpdateDate() != null, AiChatHistory::getUpdateDate, bo.getUpdateDate());
        lqw.eq(AiChatHistory::getDeleteFlag, 0);
        lqw.eq(bo.getPinnedAt() != null, AiChatHistory::getPinnedAt, bo.getPinnedAt());
        return lqw;
    }

    /**
     * 新增对话历史
     *
     * @param bo 对话历史
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(AiChatHistoryBo bo) {
        AiChatHistory add = MapstructUtils.convert(bo, AiChatHistory.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改对话历史
     *
     * @param bo 对话历史
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(AiChatHistoryBo bo) {
        AiChatHistory update = MapstructUtils.convert(bo, AiChatHistory.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(AiChatHistory entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除对话历史信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteByIds(ids) > 0;
    }
}
