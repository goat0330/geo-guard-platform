/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.data.domain.po.AdRegion;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = AdRegion.class, reverseConvertGenerate = false)
public class AdRegionBo extends BaseEntity {

    /**
     * 唯一标识符
     */
    private String id;

    /**
     * 行政区划代码（国标 GB/T 2260），6位省/市/县码，9位乡镇码，12位村/社区码
     */
    private String pcode;

    /**
     * 当前层级行政区划名称
     */
    private String name;

    /**
     * 行政层级编码：1-省级, 2-地市级, 3-县级, 4-乡镇级, 5-村级
     */
    private Integer level;

    /**
     * 行政层级名称
     */
    private String levelName;

    /**
     * 所属省份全称
     */
    private String province;

    /**
     * 所属地级市/自治州全称
     */
    private String city;

    /**
     * 所属区/县/县级市全称
     */
    private String county;

    /**
     * 所属乡镇/街道名称
     */
    private String street;

    /**
     * 行政村名称
     */
    private String village;

    /**
     * 社区名称
     */
    private String community;

    /**
     * 几何中心点
     */
    private String center;

    /**
     * 边界多边形
     */
    private String wkt;

    /**
     * 面积
     */
    private BigDecimal area;

    private Double simpWktLevel = 0.001;

}