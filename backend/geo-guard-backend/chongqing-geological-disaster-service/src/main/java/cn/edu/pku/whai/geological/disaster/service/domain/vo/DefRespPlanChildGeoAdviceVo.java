/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class DefRespPlanChildGeoAdviceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 子节点防御响应 ID；若该乡镇仅存在预警映射、不存在防御响应，则为空。
     */
    private Long defId;

    /**
     * 子节点当前响应等级；若该乡镇仅存在预警映射、不存在防御响应，则为空。
     */
    private Integer level;

    /**
     * 子节点所属乡镇/街道。
     */
    private String streets;

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
