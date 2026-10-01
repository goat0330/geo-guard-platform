/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.House;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 房间信息视图对象
 *
 * @author system
 * @date 2024
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = House.class)
public class DataHouseVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private String id;

    /**
     * 户室唯一ID
     */
    private String houseUnitId;

    /**
     * 房屋建筑代码（3010码）
     */
    private String buildingCode;

    /**
     * 建筑物名称
     */
    private String buildingName;

    /**
     * 省行政区划代码
     */
    private String provinceCode;

    /**
     * 市行政区划代码
     */
    private String cityCode;

    /**
     * 区县行政区划代码
     */
    private String countyCode;

    /**
     * 乡镇街行政区划代码
     */
    private String streetCode;

    /**
     * 社区居委会代码
     */
    private String communityCode;

    /**
     * 村行政区划代码
     */
    private String villageCode;

    /**
     * 社区网格代码
     */
    private String gridCode;

    /**
     * 街路巷地址代码
     */
    private String streetAddressCode;

    /**
     * 门楼牌号
     */
    private String doorPlateNumber;

    /**
     * 小区地址名称
     */
    private String communityName;

    /**
     * 楼栋号
     */
    private String buildingNumber;

    /**
     * 单元号
     */
    private String unitNumber;

    /**
     * 楼层号
     */
    private String floorNumber;

    /**
     * 房间号
     */
    private String roomNumber;

    /**
     * 经度（坐标x）
     */
    private BigDecimal longitude;

    /**
     * 纬度（坐标y）
     */
    private BigDecimal latitude;

    /**
     * 几何对象（PostGIS geometry）
     */
    private String geometry;

    /**
     * 是否属于588范围
     */
    private Integer pilotArea1;

    /**
     * 是否属于46范围
     */
    private Integer pilotArea2;

    /**
     * 面积
     */
    private BigDecimal area;

    /**
     * 采集时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
