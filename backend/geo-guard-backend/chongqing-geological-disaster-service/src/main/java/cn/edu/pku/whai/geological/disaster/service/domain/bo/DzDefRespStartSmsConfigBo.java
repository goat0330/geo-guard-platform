/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzDefRespStartSmsConfig;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 启动短信配置业务对象 dz_def_resp_start_sms_config
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzDefRespStartSmsConfig.class, reverseConvertGenerate = false)
public class DzDefRespStartSmsConfigBo extends BaseEntity {

    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    @NotBlank(message = "业务角色key不能为空", groups = {EditGroup.class})
    private String bizKey;

    @NotBlank(message = "业务角色名称不能为空", groups = {EditGroup.class})
    private String bizName;

    /**
     * 系统角色key列表
     */
    private List<String> roleKeyList;

    @NotBlank(message = "短信模板不能为空", groups = {EditGroup.class})
    private String smsTemplate;

    @NotNull(message = "状态不能为空", groups = {EditGroup.class})
    private Integer status;

    @NotNull(message = "排序不能为空", groups = {EditGroup.class})
    private Integer sort;

    private String remark;
}
