/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 住户信息。
 *
 * @author whai
 * @date 2026-05-20
 */
@Data
public class ResidentsInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否有住户。
     */
    private Boolean hasResidents;

    /**
     * 老人数量。
     */
    @JsonAlias("elderly_count")
    private Integer elderlyCount;

    /**
     * 儿童数量。
     */
    @JsonAlias("children_count")
    private Integer childrenCount;

    /**
     * 总人数。
     */
    @JsonAlias("total_count")
    private Integer totalCount;

    /**
     * 户主电话。
     */
    @JsonAlias("household_head_phone")
    private String householdHeadPhone;

    /**
     * 户主姓名。
     */
    @JsonAlias("household_head_name")
    private String householdHeadName;

    /**
     * 历史住户 JSON 中的预计耗时，仅用于反序列化兼容；对外不再嵌套返回。
     */
    @JsonProperty(value = "estimated_time_minutes", access = JsonProperty.Access.WRITE_ONLY)
    private Integer estimatedTimeMinutes;
}
