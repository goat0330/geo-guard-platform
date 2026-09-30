package cn.edu.pku.whai.geological.disaster.service.dify.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.AiChatHistoryDetailFeedbacksBo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.FeedBacksBo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.AiChatHistoryDetail;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.AiChatHistoryDetailFeedbacks;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.vo.AiChatHistoryDetailFeedbacksVo;
import cn.edu.pku.whai.geological.disaster.service.dify.mapper.AiChatHistoryDetailFeedbacksMapper;
import cn.edu.pku.whai.geological.disaster.service.dify.mapper.AiChatHistoryDetailMapper;
import cn.edu.pku.whai.geological.disaster.service.dify.service.IAiChatHistoryDetailFeedbacksService;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjUtil;
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
 * 对话历史记录详情反馈Service业务层处理
 *
 * @author kongweiguang
 * @date 2026-01-10
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AiChatHistoryDetailFeedbacksServiceImpl implements IAiChatHistoryDetailFeedbacksService {

    private final AiChatHistoryDetailFeedbacksMapper baseMapper;
    private final AiChatHistoryDetailMapper aiChatHistoryDetailMapper;

    /**
     * 查询对话历史记录详情反馈
     *
     * @param id 主键
     * @return 对话历史记录详情反馈
     */
    @Override
    public AiChatHistoryDetailFeedbacksVo queryById(String id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询对话历史记录详情反馈列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 对话历史记录详情反馈分页列表
     */
    @Override
    public TableDataInfo<AiChatHistoryDetailFeedbacksVo> queryPageList(AiChatHistoryDetailFeedbacksBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<AiChatHistoryDetailFeedbacks> lqw = buildQueryWrapper(bo);
        Page<AiChatHistoryDetailFeedbacksVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的对话历史记录详情反馈列表
     *
     * @param bo 查询条件
     * @return 对话历史记录详情反馈列表
     */
    @Override
    public List<AiChatHistoryDetailFeedbacksVo> queryList(AiChatHistoryDetailFeedbacksBo bo) {
        LambdaQueryWrapper<AiChatHistoryDetailFeedbacks> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<AiChatHistoryDetailFeedbacks> buildQueryWrapper(AiChatHistoryDetailFeedbacksBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<AiChatHistoryDetailFeedbacks> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getUserId() != null, AiChatHistoryDetailFeedbacks::getUserId, bo.getUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getHistoryId()), AiChatHistoryDetailFeedbacks::getHistoryId, bo.getHistoryId());
        lqw.eq(StringUtils.isNotBlank(bo.getHistoryDetailId()), AiChatHistoryDetailFeedbacks::getHistoryDetailId, bo.getHistoryDetailId());
        lqw.eq(bo.getRating() != null, AiChatHistoryDetailFeedbacks::getRating, bo.getRating());
        lqw.eq(StringUtils.isNotBlank(bo.getContent()), AiChatHistoryDetailFeedbacks::getContent, bo.getContent());
        lqw.eq(bo.getCreateDate() != null, AiChatHistoryDetailFeedbacks::getCreateDate, bo.getCreateDate());
        lqw.eq(bo.getUpdateDate() != null, AiChatHistoryDetailFeedbacks::getUpdateDate, bo.getUpdateDate());
        return lqw;
    }

    /**
     * 新增对话历史记录详情反馈
     *
     * @param bo 对话历史记录详情反馈
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(AiChatHistoryDetailFeedbacksBo bo) {
        AiChatHistoryDetailFeedbacks add = MapstructUtils.convert(bo, AiChatHistoryDetailFeedbacks.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改对话历史记录详情反馈
     *
     * @param bo 对话历史记录详情反馈
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(AiChatHistoryDetailFeedbacksBo bo) {
        AiChatHistoryDetailFeedbacks update = MapstructUtils.convert(bo, AiChatHistoryDetailFeedbacks.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(AiChatHistoryDetailFeedbacks entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除对话历史记录详情反馈信息
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

    @Override
    public List<AiChatHistoryDetailFeedbacksVo> queryListByIds(List<String> ids) {
        LambdaQueryWrapper<AiChatHistoryDetailFeedbacks> lqw = Wrappers.lambdaQuery();
        lqw.in(AiChatHistoryDetailFeedbacks::getHistoryDetailId, ids);
        lqw.isNotNull(AiChatHistoryDetailFeedbacks::getRating);
        return baseMapper.selectVoList(lqw);
    }

    @Override
    public void handle(FeedBacksBo bo) {
        String detailId = bo.getDetailId();
        Long userId = LoginHelper.getUserId();
        AiChatHistoryDetail aiChatHistoryDetail = aiChatHistoryDetailMapper.selectOne(Wrappers.<AiChatHistoryDetail>lambdaQuery()
                .eq(AiChatHistoryDetail::getId, detailId)
                .eq(AiChatHistoryDetail::getUserId, userId));
        if (ObjUtil.isNull(aiChatHistoryDetail)) {
            throw new ServiceException("未找到当前消息");
        }

        AiChatHistoryDetailFeedbacks feedback = baseMapper.selectOne(Wrappers.<AiChatHistoryDetailFeedbacks>lambdaQuery()
                .eq(AiChatHistoryDetailFeedbacks::getHistoryDetailId, detailId)
                .eq(AiChatHistoryDetailFeedbacks::getUserId, userId));

        if (ObjUtil.isNull(feedback)) {
            feedback = new AiChatHistoryDetailFeedbacks();
            feedback.setId(IdUtil.fastUUID());
            feedback.setUserId(userId);
            feedback.setHistoryId(aiChatHistoryDetail.getHistoryId());
            feedback.setHistoryDetailId(aiChatHistoryDetail.getId());
            feedback.setContent(bo.getContent());
            feedback.setRating(bo.getRating());
            feedback.setCreateDate(new Date());
            feedback.setUpdateDate(new Date());
            baseMapper.insert(feedback);
        } else {
            feedback.setContent(bo.getContent());
            feedback.setRating(bo.getRating());
            feedback.setUpdateDate(new Date());
            baseMapper.updateById(feedback);
        }
    }
}
