package cn.edu.pku.whai.geological.disaster.service.dify.domain.vo;

import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import cn.edu.pku.whai.geological.disaster.service.dify.domain.po.AiChatHistory;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 对话历史视图对象 ai_chat_history
 *
 * @author kongweiguang
 * @date 2026-01-10
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = AiChatHistory.class)
public class AiChatHistoryVo implements Serializable {

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
     * 区分不同对话
     */
    @ExcelProperty(value = "区分不同对话")
    private String type;

    /**
     * 当次会话标题
     */
    @ExcelProperty(value = "当次会话标题")
    private String title;

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

    /**
     * 是否删除（0：未删除 1：已删除）
     */
    @ExcelProperty(value = "是否删除", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=：未删除,1=：已删除")
    private Integer deleteFlag;

    private Integer pinnedAt;

    private AiChatHistoryDetailVo lastDetail;
}
