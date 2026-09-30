/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.system.domain.vo.SysUserVo;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RoleUserVo extends SysUserVo {

    /**
     * 角色ID集合，逗号分隔
     */
    private String matchedRoleIds;

    /**
     * 角色标识集合，逗号分隔
     */
    private String matchedRoleKeys;

    /**
     * 角色名称集合，逗号分隔
     */
    private String matchedRoleNames;

    /**
     * 专家类型（1：地质专家，2：水文专家）
     */
    @ExcelProperty(value = "专家类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1=地质专家，2=水文专家")
    private Integer expertType;

    /**
     * 简介
     */
    @ExcelProperty(value = "简介")
    private String introduction;

    /**
     * 会商次数
     */
    @ExcelProperty(value = "会商次数")
    private Integer meetingCount;

    /**
     * 在线状态(0：离线，1：在线
     */
    private Integer online = 0;
}
