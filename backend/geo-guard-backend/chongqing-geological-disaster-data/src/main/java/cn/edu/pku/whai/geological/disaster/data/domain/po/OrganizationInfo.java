/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 组织机构信息对象 v_organization_info
 **/
@Data
@TableName("v_organization_info")
public class OrganizationInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 组织机构唯一主键ID（支持长字符UUID/业务编码）
     */
    @TableId(value = "id")
    private String id;

    /**
     * 组织机构全称（如"武当山特区自然资源和规划局"）
     */
    private String organizationFullName;

    /**
     * 父级组织机构ID（构建省-市-县-乡树形层级）
     */
    private String parentOrganizationId;

    /**
     * 组织机构简称（如"武当山自规局"）
     */
    private String organizationShortName;

    /**
     * 关联行政区划ID（关联行政区划表，精准定位属地）
     */
    private String administrativeRegionId;

    /**
     * 组织机构编码（行业/系统内统一编码）
     */
    private String organizationCode;

    /**
     * 排序号（前端展示/数据查询的排序顺序）
     */
    private Long sortNumber;

    /**
     * 层级（1=省级、2=市级、3=县级、4=乡级等）
     */
    private Long organizationLevel;

    /**
     * 联系电话（支持座机/手机号，多号码用分隔符拼接）
     */
    private String contactPhone;

    /**
     * 联系人姓名（组织机构对接人）
     */
    private String contactPerson;

    /**
     * 电子邮箱（支持多个邮箱，用分隔符拼接）
     */
    private String emailAddress;

    /**
     * 详细地址（如"湖北省十堰市武当山特区永乐路1号"）
     */
    private String detailedAddress;

    /**
     * 图片/LOGO路径（存储文件服务器路径/URL）
     */
    private String logoImagePath;

    /**
     * 传真号码（办公传真）
     */
    private String faxNumber;

    /**
     * 创建人（系统操作人ID/姓名）
     */
    private String createdBy;

    /**
     * 创建时间（含时分秒的时间戳）
     */
    private Date createdTime;

    /**
     * 更新人（最后修改人ID/姓名）
     */
    private String updatedBy;

    /**
     * 更新时间（最后修改时间戳）
     */
    private Date updatedTime;

    /**
     * 备注（组织机构额外说明信息）
     */
    private String remark;

    /**
     * 外部系统组织机构ID（对接其他系统的关联ID）
     */
    private String externalOrganizationId;


}
