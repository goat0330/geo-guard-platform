package cn.edu.pku.whai.geological.disaster.service.dify.domain.vo;

import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.AiChatHistoryDetailFeedbacks;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 对话历史记录详情反馈视图对象 ai_chat_history_detail_feedbacks
 *
 * @author kongweiguang
 * @date 2026-01-10
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = AiChatHistoryDetailFeedbacks.class)
public class AiChatHistoryDetailFeedbacksVo implements Serializable {

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
     * 历史对话id
     */
    @ExcelProperty(value = "历史对话id")
    private String historyId;

    /**
     * 对话详情id
     */
    @ExcelProperty(value = "对话详情id")
    private String historyDetailId;

    /**
     * 评价（1：喜欢，2：不喜欢）
     */
    @ExcelProperty(value = "评价", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1=：喜欢，2：不喜欢")
    private Integer rating;

    /**
     * 评价内容
     */
    @ExcelProperty(value = "评价内容")
    private String content;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createDate;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateDate;


}
