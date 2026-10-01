/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;


import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.data.domain.po.SlopeUnit;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 斜坡单元业务对象 data_slope_unit
 *
 * @author kongweiguang
 * @date 2026-01-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SlopeUnit.class, reverseConvertGenerate = false)
public class SlopeUnitBo extends BaseEntity {

    /**
     * 唯一标识符
     */
    @NotBlank(message = "唯一标识符不能为空", groups = {EditGroup.class})
    private String id;

    /**
     * 斜坡单元名称
     */
    private String name;

    /**
     * 是否属于588范围
     */
    private Integer pilotArea1;

    /**
     * 是否属于46范围
     */
    private Integer pilotArea2;

    /**
     * 是否属于城区低风险范围
     */
    private Integer urbanArea;

    /**
     * 所属省份
     */
    private String province;

    /**
     * 所属地级市
     */
    private String city;

    /**
     * 所属区/县
     */
    private String county;

    /**
     * 所属乡镇/街道
     */
    private String street;

    /**
     * 所属行政村
     */
    private String village;

    /**
     * 所属社区（若适用）
     */
    private String community;

    /**
     * 中心点坐标
     */
    private String center;

    /**
     * 边界多边形
     */
    private String wkt;

    /**
     * $column.columnComment
     */
    private BigDecimal area;

    private Integer riskLevel;
    private List<Integer> dynamicRiskLevelList;

    /**
     * 动态风险值
     */
    private Integer dynamicRiskValue;

    /**
     * 省编码
     */
    private String provinceCode;

    /**
     * 市编码
     */
    private String cityCode;

    /**
     * 区县编码
     */
    private String countyCode;

    /**
     * 街道/乡镇编码
     */
    private String streetCode;

    /**
     * 村/社区编码
     */
    private String villageCode;

}
