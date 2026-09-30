/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class DefRespPlanStreetGeoAdviceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 乡镇/街道名称。
     */
    private String streets;

    /**
     * 当前关联的区域响应等级；未关联时返回 null。
     */
    private Integer level;

    /**
     * 根据当前有效预警计算得到的地象建议等级。
     */
    private Integer geoAdviceLevel;

    /**
     * 地象建议来源预警 ID。
     */
    private Long alarmId;

    /**
     * 地象建议来源预警创建时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date alarmCreateDate;
}
