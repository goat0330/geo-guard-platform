package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 灾害处置对象 dz_task_handle
 *
 * @author kongweiguang
 * @date 2026-01-29
 */
@Data
@TableName("dz_task_handle")
public class DzTaskHandle implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id")
    private Long id;

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
     * 中心点坐标
     */
    private String center;

    /**
     * 详细地址
     */
    private String detailedAddress;

    /**
     * 事件类型（1：滑坡 2：崩塌 3：地面塌陷 4：泥石流 5：危岩  100：其他）
     */
    private Integer eventType;

    /**
     * 事件级别，统一采用数值越大表示灾情规模越大（1：小型 2：中型 3：大型 4：特大型）
     */
    private Integer eventLevel;

    /**
     * 响应状态（0：未启动响应；统一采用数值越大表示响应等级越高：1：4级响应 2：3级响应 3：2级响应 4：1级响应）
     */
    private Integer respStatus;

    /**
     * 处理进度（1：应急调查 2：会商研判 3：方案接入 4：响应执行 5：闭环归档）
     */
    private Integer handleProcess;

    /**
     * 影响范围
     */
    private String influenceScope;

    /**
     * 负责人
     */
    private String responsiblePerson;

    /**
     * 负责人手机号
     */
    private String responsiblePersonPhone;

    /**
     * 负责人角色
     */
    private String responsiblePersonRole;

    /**
     * 上报人
     */
    private String reporter;

    /**
     * 上报时间
     */
    private Date reporterDate;

    /**
     * 斜坡单元id
     */
    private String slopeUnitId;

    /**
     * 群众撤离（0：不涉及，1：涉及）
     */
    private Integer peopleLeave;

    /**
     * 是否跳过: 0.否 1.是
     */
    private Integer isSkipped;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;


}
