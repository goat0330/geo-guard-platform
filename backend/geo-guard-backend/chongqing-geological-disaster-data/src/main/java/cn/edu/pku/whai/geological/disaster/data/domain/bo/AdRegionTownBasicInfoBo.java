/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 按乡镇名称批量查询行政区划基础信息入参。
 */
@Data
public class AdRegionTownBasicInfoBo {

    /**
     * 乡镇/街道名称列表。
     */
    @NotEmpty(message = "townNames不能为空")
    private List<String> townNames;
}
