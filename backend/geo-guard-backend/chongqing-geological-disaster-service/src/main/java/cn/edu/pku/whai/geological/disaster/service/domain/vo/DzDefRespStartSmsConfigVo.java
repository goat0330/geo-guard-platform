/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzDefRespStartSmsConfig;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 启动短信配置视图对象 dz_def_resp_start_sms_config
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DzDefRespStartSmsConfig.class)
public class DzDefRespStartSmsConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String bizKey;

    private String bizName;

    /**
     * 原始JSON配置
     */
    private String roleKeys;

    /**
     * 解析后的角色key列表
     */
    private List<String> roleKeyList;

    /**
     * 解析后的角色名称列表
     */
    private List<String> roleNameList;

    private String smsTemplate;

    private Integer status;

    private Integer sort;

    private String remark;

    private Long createBy;

    private Date createTime;

    private Long updateBy;

    private Date updateTime;

    private String delFlag;
}
