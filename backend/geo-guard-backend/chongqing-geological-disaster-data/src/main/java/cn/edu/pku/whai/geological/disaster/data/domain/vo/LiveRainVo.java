/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class LiveRainVo {
    /**
     * 地区名称
     */
    private String name;

    /**
     * 坐标
     */
    private List<Double> coordinates;

    /**
     * 001
     */
    private Double value;
}
