/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.List;

@Data
public class TaskDistPushVo {

    /**
     * 用户名称列表
     */
    @ExcelProperty(value = "用户名称列表")
    private List<String> pushName;

    /**
     * 角色列表
     */
    @ExcelProperty(value = "角色")
    private List<String> role;
    
    /**
     * 高风险斜坡单元数量
     */
    @ExcelProperty(value = "高风险斜坡单元数量")
    private Integer highCount;

    /**
     * 极高风险斜坡单元数量
     */
    @ExcelProperty(value = "极高风险斜坡单元数量")
    private Integer veryHighCount;
}
