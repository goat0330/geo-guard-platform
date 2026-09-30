package cn.edu.pku.whai.geological.disaster.service.dify.service.impl;


import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.AiChatHistoryDetailBo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.AiChatHistoryDetail;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.vo.AiChatHistoryDetailFeedbacksVo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.vo.AiChatHistoryDetailVo;
import cn.edu.pku.whai.geological.disaster.service.dify.mapper.AiChatHistoryDetailMapper;
import cn.edu.pku.whai.geological.disaster.service.dify.service.IAiChatHistoryDetailFeedbacksService;
import cn.edu.pku.whai.geological.disaster.service.dify.service.IAiChatHistoryDetailService;
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
 * 对话历史记录详情Service业务层处理
 *
 * @author kongweiguang
 * @date 2026-01-10
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AiChatHistoryDetailServiceImpl implements IAiChatHistoryDetailService {

    private final AiChatHistoryDetailMapper baseMapper;
    private final IAiChatHistoryDetailFeedbacksService aiChatHistoryDetailFeedbacksService;

    /**
     * 查询对话历史记录详情
     *
     * @param id 主键
     * @return 对话历史记录详情
     */
    @Override
    public AiChatHistoryDetailVo queryById(String id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询对话历史记录详情列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 对话历史记录详情分页列表
     */
    @Override
    public TableDataInfo<AiChatHistoryDetailVo> queryPageList(AiChatHistoryDetailBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<AiChatHistoryDetail> lqw = buildQueryWrapper(bo);
        Page<AiChatHistoryDetailVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);

        List<AiChatHistoryDetailVo> records = result.getRecords();
        if (ObjUtil.isEmpty(records)) {
            return TableDataInfo.build(result);
        }

        List<String> ids = records.stream().map(AiChatHistoryDetailVo::getId).toList();
        List<AiChatHistoryDetailFeedbacksVo> feedbacks = aiChatHistoryDetailFeedbacksService.queryListByIds(ids);

        if (ObjUtil.isEmpty(feedbacks)) {
            return TableDataInfo.build(result);
        }

        Map<String, List<AiChatHistoryDetailFeedbacksVo>> imap = CollStreamUtil.groupByKey(feedbacks, AiChatHistoryDetailFeedbacksVo::getHistoryDetailId);

        records.forEach(e -> e.setFeedbacks(imap.get(e.getId())));

        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的对话历史记录详情列表
     *
     * @param bo 查询条件
     * @return 对话历史记录详情列表
     */
    @Override
    public List<AiChatHistoryDetailVo> queryList(AiChatHistoryDetailBo bo) {
        LambdaQueryWrapper<AiChatHistoryDetail> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<AiChatHistoryDetail> buildQueryWrapper(AiChatHistoryDetailBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<AiChatHistoryDetail> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getUserId() != null, AiChatHistoryDetail::getUserId, bo.getUserId());
        lqw.eq(bo.getHistoryId() != null, AiChatHistoryDetail::getHistoryId, bo.getHistoryId());
        lqw.eq(StringUtils.isNotBlank(bo.getQuery()), AiChatHistoryDetail::getQuery, bo.getQuery());
        lqw.eq(StringUtils.isNotBlank(bo.getAnswer()), AiChatHistoryDetail::getAnswer, bo.getAnswer());
        lqw.eq(bo.getCreateDate() != null, AiChatHistoryDetail::getCreateDate, bo.getCreateDate());
        return lqw;
    }

    /**
     * 新增对话历史记录详情
     *
     * @param bo 对话历史记录详情
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(AiChatHistoryDetailBo bo) {
        AiChatHistoryDetail add = MapstructUtils.convert(bo, AiChatHistoryDetail.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改对话历史记录详情
     *
     * @param bo 对话历史记录详情
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(AiChatHistoryDetailBo bo) {
        AiChatHistoryDetail update = MapstructUtils.convert(bo, AiChatHistoryDetail.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(AiChatHistoryDetail entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除对话历史记录详情信息
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
