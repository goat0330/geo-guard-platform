package cn.edu.pku.whai.geological.disaster.service.dify.domain.vo;

import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.AiChatHistoryDetail;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.imfangs.dify.client.model.file.FileInfo;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * 对话历史记录详情视图对象 ai_chat_history_detail
 *
 * @author kongweiguang
 * @date 2026-01-10
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = AiChatHistoryDetail.class)
public class AiChatHistoryDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @ExcelProperty(value = "id")
    private String id;

    /**
     * 用户id
     */
    @ExcelProperty(value = "用户id")
    private Long userId;

    /**
     * 对话id
     */
    @ExcelProperty(value = "对话id")
    private String historyId;

    /**
     * 对话传入额外参数
     */
    @ExcelProperty(value = "对话传入额外参数")
    private Map<String, Object> inputs;

    /**
     * 本次对话请求携带的附件
     */
    private List<FileInfo> files;

    /**
     * 问题
     */
    @ExcelProperty(value = "问题")
    private String query;

    /**
     * 答案
     */
    @ExcelProperty(value = "答案")
    private String answer;

    /**
     * 额外响应数据
     */
    @ExcelProperty(value = "额外响应数据")
    private Map<String, Object> messageMetadata;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createDate;

    private List<AiChatHistoryDetailFeedbacksVo> feedbacks;

}
