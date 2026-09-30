/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import lombok.Data;

import java.util.List;

@Data
public class DzRiskAssessmentStatBo {
    private Integer offset;
    private String slopeUnitId;
    /**
     * 所属省份
     */
    private String province;

    /**
     * 所属地级市
     */
    private String city;

    /**
     * 所属区/县
     */
    private String county;

    /**
     * 所属乡镇/街道
     */
    private String street;

    /**
     * 所属行政村
     */
    private String village;

    /**
     * 所属社区（若适用）
     */
    private String community;

    /**
     * 是否属于588范围
     */
    private Integer pilotArea1;

    /**
     * 是否属于46范围
     */
    private Integer pilotArea2;

    /**
     * 当前用户绑定的行政区划编码集合，用于多区划权限过滤。
     */
    private List<String> adRegionIds;
}
