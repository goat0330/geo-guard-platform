package cn.edu.pku.whai.geological.disaster.service.dify.service;


import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.bo.AiChatHistoryBo;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.vo.AiChatHistoryVo;

import java.util.Collection;
import java.util.List;

/**
 * 对话历史Service接口
 *
 * @author kongweiguang
 * @date 2026-01-10
 */
public interface IAiChatHistoryService {

    /**
     * 查询对话历史
     *
     * @param id 主键
     * @return 对话历史
     */
    AiChatHistoryVo queryById(String id);

    /**
     * 分页查询对话历史列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 对话历史分页列表
     */
    TableDataInfo<AiChatHistoryVo> queryPageList(AiChatHistoryBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的对话历史列表
     *
     * @param bo 查询条件
     * @return 对话历史列表
     */
    List<AiChatHistoryVo> queryList(AiChatHistoryBo bo);

    /**
     * 新增对话历史
     *
     * @param bo 对话历史
     * @return 是否新增成功
     */
    Boolean insertByBo(AiChatHistoryBo bo);

    /**
     * 修改对话历史
     *
     * @param bo 对话历史
     * @return 是否修改成功
     */
    Boolean updateByBo(AiChatHistoryBo bo);

    /**
     * 校验并批量删除对话历史信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
