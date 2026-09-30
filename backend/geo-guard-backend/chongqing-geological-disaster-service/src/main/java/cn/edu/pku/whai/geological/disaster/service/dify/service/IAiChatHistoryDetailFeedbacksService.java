package cn.edu.pku.whai.geological.disaster.service.dify.service;


import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.AiChatHistoryDetailFeedbacksBo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.FeedBacksBo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.FeedBacksCommentBo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.vo.AiChatHistoryDetailFeedbacksVo;

import java.util.Collection;
import java.util.List;

/**
 * 对话历史记录详情反馈Service接口
 *
 * @author kongweiguang
 * @date 2026-01-10
 */
public interface IAiChatHistoryDetailFeedbacksService {

    /**
     * 查询对话历史记录详情反馈
     *
     * @param id 主键
     * @return 对话历史记录详情反馈
     */
    AiChatHistoryDetailFeedbacksVo queryById(String id);

    /**
     * 分页查询对话历史记录详情反馈列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 对话历史记录详情反馈分页列表
     */
    TableDataInfo<AiChatHistoryDetailFeedbacksVo> queryPageList(AiChatHistoryDetailFeedbacksBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的对话历史记录详情反馈列表
     *
     * @param bo 查询条件
     * @return 对话历史记录详情反馈列表
     */
    List<AiChatHistoryDetailFeedbacksVo> queryList(AiChatHistoryDetailFeedbacksBo bo);

    /**
     * 新增对话历史记录详情反馈
     *
     * @param bo 对话历史记录详情反馈
     * @return 是否新增成功
     */
    Boolean insertByBo(AiChatHistoryDetailFeedbacksBo bo);

    /**
     * 修改对话历史记录详情反馈
     *
     * @param bo 对话历史记录详情反馈
     * @return 是否修改成功
     */
    Boolean updateByBo(AiChatHistoryDetailFeedbacksBo bo);

    /**
     * 校验并批量删除对话历史记录详情反馈信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);

    List<AiChatHistoryDetailFeedbacksVo> queryListByIds(List<String> ids);

    void handle(FeedBacksBo bo);

}
