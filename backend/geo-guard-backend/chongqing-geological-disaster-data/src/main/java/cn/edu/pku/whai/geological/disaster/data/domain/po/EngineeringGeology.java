/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 地质信息
 *
 * @author lizheng
 */
// 工程地质
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
public class EngineeringGeology {
    private String rockGroup;         // 岩组
    private String characteristics;   // 地质特征
    private String projectName;       // 名称
}
