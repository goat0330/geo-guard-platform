/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 地质信息
 *
 * @author lizheng
 */
// 地层数据
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Stratum {
    private String code;              // 地层代号
    private String label;             // 地层标注
    private String stratumEra;        // 地层界
    private String stratumSystem;     // 地层系
    private String stratumSeries;     // 地层统
    private String stratumMember;     // 地层段
    private String description;       // 地层整体描述
    private String stratumGroup;      // 地层组
    private String lithology;         // 地层岩性
}
