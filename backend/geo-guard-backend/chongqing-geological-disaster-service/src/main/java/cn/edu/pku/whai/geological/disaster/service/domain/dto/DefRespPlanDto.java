/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class DefRespPlanDto {

    /**
     * 主键
     */
    private Long id;

    /**
     * 编码
     */
    private String code;

    /**
     * 名称
     */
    private String name;

    /**
     * 类型
     */
    private Integer type;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 方案id
     */
    private Long handleId;

    /**
     * 按乡镇级区域防御方案 id 过滤（单点关联的 regId 仅此类）
     */
    private Long regId;

    /**
     * 中心点
     */
    private String center;

    /**
     * 触发条件
     */
    private String triggerCondition;

    /**
     * 城市
     */
    private String city;

    /**
     * 街道
     */
    private String streets;

    /**
     * 责任单位
     */
    private String responsibilityUnit;

    /**
     * 责任人
     */
    private String responsiblePerson;

    /**
     * 责任人电话
     */
    private String responsiblePersonPhone;

    /**
     * 响应等级
     */
    private Integer level;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 修改时间
     */
    private Date updateDate;

    /**
     * 关键字
     */
    private String userKeyword;

    /**
     * 开始时间
     */
    private Date beginTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 查询的状态
     */
    private List<Integer> process;

    /**
     * 1独立 2关联
     */
    private Integer relation;

    /**
     * 区域层级：1 县级 2 乡镇；不传则不限制
     */
    private Integer regionScopeType;

    /**
     * 按父级县级 id 查下属乡镇行
     */
    private Long parentDefId;

    /**
     * 按当前/指定轮次过滤
     */
    private Integer currentRoundNo;

    /**
     * 是否需要鉴权: 0:不需要 1:需要
     */
    private Integer isVerify;
}
