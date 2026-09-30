package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzExpertExt;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 专家扩展信息视图对象 dz_expert_ext
 *
 * @author kongweiguang
 * @date 2026-02-03
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DzExpertExt.class)
public class DzExpertExtVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @ExcelProperty(value = "id")
    private Long id;

    /**
     * 用户id
     */
    @ExcelProperty(value = "用户id")
    private Long userId;

    /**
     * 专家类型（1：地质专家，2：水文专家）
     */
    @ExcelProperty(value = "专家类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1=地质专家，2水文专家")
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


}
