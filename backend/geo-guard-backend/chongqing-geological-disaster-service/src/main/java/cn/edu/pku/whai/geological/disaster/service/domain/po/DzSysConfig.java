/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 地灾侧系统参数配置映射，实际表为 sys_config。
 */
@Data
@TableName("sys_config")
public class DzSysConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "config_id")
    private Long configId;

    private String tenantId;

    private String configName;

    private String configKey;

    private String configValue;

    private String configType;

    private Long createDept;

    private Long createBy;

    private Date createTime;

    private Long updateBy;

    private Date updateTime;

    private String remark;
}
