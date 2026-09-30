package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 业务消息通知对象 dz_msg_notice
 *
 * @author kongweiguang
 * @date 2026-04-07
 */
@Data
@TableName("dz_msg_notice")
public class DzMsgNotice implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 正文
     */
    private String content;

    /**
     * 类型
     */
    private String type;

    /**
     * 状态（0：未处理，1：已处理）
     */
    private Integer status;

    /**
     * 业务数据
     */
    private String bizData;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;

    /**
     * user_id
     */
    private Long userId;


}
