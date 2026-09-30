/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzMsgNotice;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * 业务消息通知业务对象 dz_msg_notice
 *
 * @author kongweiguang
 * @date 2026-04-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzMsgNotice.class, reverseConvertGenerate = false)
public class DzMsgNoticeBo extends BaseEntity {

    /**
     * id
     */
    private Long id;
    private List<Long> ids;
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
