package cn.edu.pku.whai.geological.disaster.service.dify.domain.bo;


import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.AiChatHistoryDetail;
import io.github.imfangs.dify.client.model.file.FileInfo;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 对话历史记录详情业务对象 ai_chat_history_detail
 *
 * @author kongweiguang
 * @date 2026-01-10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = AiChatHistoryDetail.class, reverseConvertGenerate = false)
public class AiChatHistoryDetailBo extends BaseEntity {

    /**
     * id
     */
    private String id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 对话id
     */
    private String historyId;

    /**
     * 对话传入额外参数
     */
    private Map<String, Object> inputs;

    /**
     * 本次对话请求携带的附件
     */
    private List<FileInfo> files;

    /**
     * 问题
     */
    private String query;

    /**
     * 答案
     */
    private String answer;

    /**
     * 额外响应数据
     */
    private Map<String, Object> messageMetadata;

    /**
     * 创建时间
     */
    private Date createDate;


}
