package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzMsgNotice;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 业务消息通知视图对象 dz_msg_notice
 *
 * @author kongweiguang
 * @date 2026-04-07
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DzMsgNotice.class)
public class DzMsgNoticeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @ExcelProperty(value = "id")
    private Long id;

    /**
     * 标题
     */
    @ExcelProperty(value = "标题")
    private String title;

    /**
     * 正文
     */
    @ExcelProperty(value = "正文")
    private String content;

    /**
     * 类型
     */
    @ExcelProperty(value = "类型")
    private String type;

    /**
     * 状态（0：未处理，1：已处理）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=：未处理，1：已处理")
    private Integer status;

    /**
     * 业务数据
     */
    @ExcelProperty(value = "业务数据")
    private String bizData;

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
     * user_id
     */
    @ExcelProperty(value = "user_id")
    private Long userId;


}
