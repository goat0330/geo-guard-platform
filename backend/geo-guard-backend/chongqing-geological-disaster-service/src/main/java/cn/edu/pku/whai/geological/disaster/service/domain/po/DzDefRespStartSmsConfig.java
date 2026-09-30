/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 启动短信配置对象 dz_def_resp_start_sms_config
 */
@Data
@TableName("dz_def_resp_start_sms_config")
public class DzDefRespStartSmsConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /**
     * 业务角色key
     */
    private String bizKey;

    /**
     * 业务角色名称
     */
    private String bizName;

    /**
     * 系统角色key列表，JSON数组字符串
     */
    private String roleKeys;

    /**
     * 短信模板
     */
    private String smsTemplate;

    /**
     * 状态：1启用 0停用
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建人
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新人
     */
    private Long updateBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 删除标志：0存在 1删除
     */
    private String delFlag;
}
