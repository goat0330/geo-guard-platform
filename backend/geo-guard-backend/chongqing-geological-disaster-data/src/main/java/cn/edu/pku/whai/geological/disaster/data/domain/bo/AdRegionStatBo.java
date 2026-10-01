/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdRegionStatBo {
    /**
     * 唯一标识符
     */
    @NotNull(message = "id不能为空")
    private String id;
    /**
     * 是否属于588范围
     */
    private Integer pilotArea1;

    /**
     * 是否属于46范围
     */
    private Integer pilotArea2;
}
