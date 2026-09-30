/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 处置管理详情内容对象 dz_task_handle_detail_content
 */
@Data
@TableName("dz_task_handle_detail_content")
public class DzTaskHandleDetailContent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 业务主键
     */
    private Long bizId;

    /**
     * 业务类型：1-dz_task_handle，2-dz_def_resp_plan
     */
    private Integer bizType;

    /**
     * 内容类型：1-应急调查报告，2-撤离方案，3-防御响应方案，4-初始报告，5-最终报告，6-复盘报告，7-会商确认
     */
    private Integer contentType;

    /**
     * 内容文本
     */
    private String planContent;

    /**
     * 内容结构化JSON
     */
    private String planContentJson;

    /**
     * 专家建议/提示词
     */
    private String suggest;

    /**
     * 主持人Id
     */
    private Long userId;

    /**
     * 主持人姓名
     */
    private String userName;

    /**
     * 轮次
     */
    private Integer roundNo;

    /**
     * 删除状态：0-未删除，1-已删除
     */
    private Integer deleted;

    /**
     * 是否最新：0-否，1-是
     */
    private Integer isLatest;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 状态：0-待提交，1-已提交，2-通过，3-预警同步
     */
    private Integer status;

    /**
     * 更新时间
     */
    private Date updateDate;
}
