/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 地质信息
 *
 * @author lizheng
 */
// 水文地质
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HydroGeology {
    private String aquiferRock;       // 含水岩层
    private String aquiferType;       // 含水层类型
    private String waterAbundance;    // 富水程度
    private String rockGroupEra;      // 岩组时代
    private String distributionPos;   // 分布位置
    private String burialConditions;  // 埋藏条件
}
