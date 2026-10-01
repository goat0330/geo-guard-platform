/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 地质信息对应行政区划
 *
 * @author lizheng
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
public class GeologyAdRegionVo {

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
}
