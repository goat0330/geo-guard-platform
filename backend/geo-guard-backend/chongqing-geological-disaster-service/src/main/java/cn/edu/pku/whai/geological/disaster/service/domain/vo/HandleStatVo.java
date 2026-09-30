/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.vo.RiskZoneStatVo;
import lombok.Data;

import java.util.List;

@Data
public class HandleStatVo {
    /**
     * 灾害点数量
     */
    private Long hazardPointCount;
    /**
     * 风险区域等级列表
     */
    private List<RiskZoneStatVo> riskZoneLevelList;
    /**
     * 风险区域数量
     */
    private Integer riskZoneCount;
}
