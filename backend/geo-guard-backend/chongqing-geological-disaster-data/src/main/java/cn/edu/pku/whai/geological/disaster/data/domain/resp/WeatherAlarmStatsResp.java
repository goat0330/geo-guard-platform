/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.resp;

import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class WeatherAlarmStatsResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 涉及总面积（单位与 data_slope_unit.area 一致）
     */
    private BigDecimal totalArea;

    /**
     * 涉及乡镇数量
     */
    private Integer streetCount;

    /**
     * 涉及斜坡单元数量
     */
    private Integer slopeUnitCount;

    /**
     * 告警时间
     */
    private Date createDate;

    private List<String> messages;

    private List<AdRegionVo> adRegionVoList;
}
