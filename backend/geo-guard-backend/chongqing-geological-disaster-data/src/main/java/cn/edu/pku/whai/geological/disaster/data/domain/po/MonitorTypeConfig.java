/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 监测类型配置对象 v_monitor_type_config
 **/
@Data

@TableName("v_monitor_type_config")
public class MonitorTypeConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 监测类型配置唯一主键ID（业务唯一标识）
     */
    @TableId(value = "id")
    private String id;

    /**
     * 父级ID（构建监测类型层级结构：大类-小类）
     */
    private String parentId;

    /**
     * 监测内容（如"滑坡倾角监测、地面沉降监测"）
     */
    private String monitoringContent;

    /**
     * 监测方法（如"自动监测、人工巡检、GNSS监测"）
     */
    private String monitoringMethod;

    /**
     * 监测类型编码（核心业务编码：01=位移、02=倾角）
     */
    private String monitoringTypeCode;

    /**
     * 参数名称（如"倾角值、位移量、降雨量"）
     */
    private String parameterName;

    /**
     * 参数类型（如"数值型、字符型、布尔型"）
     */
    private String parameterType;

    /**
     * 扩展属性（JSON格式存储额外配置参数）
     */
    private String extendedAttributes;

    /**
     * 排序号（原字段ORBER拼写错误，修正为标准命名）
     */
    private Long sortNumber;

    /**
     * 描述说明（监测类型/参数的详细说明）
     */
    private String description;

    /**
     * 创建人（原字段CREARTE_BY拼写错误，修正为标准命名）
     */
    private String createdBy;

    /**
     * 创建时间（原字段CREARTE_TIME拼写错误，修正为标准命名）
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
     * 监测内容编码（固定2位字符：01=滑坡、04=塌陷）
     */
    private String monitoringContentCode;

    /**
     * 备注（额外说明信息）
     */
    private String remark;

    /**
     * 层级（1=一级分类、2=二级分类等）
     */
    private Long monitoringTypeLevel;

    /**
     * 字段描述（参数/类型对应的字段说明）
     */
    private String fieldDescription;

    /**
     * 最大值（参数测量上限，如倾角最大值30.00°）
     */
    private BigDecimal parameterMaxValue;

    /**
     * 最小值（参数测量下限，如倾角最小值0.00°）
     */
    private BigDecimal parameterMinValue;

    /**
     * 警戒值（预警触发阈值，如倾角警戒值15.00°）
     */
    private BigDecimal warningThresholdValue;

    /**
     * 计量单位（如"°（度）、mm（毫米）、mm/h（毫米/小时）"）
     */
    private String measurementUnit;

    /**
     * 默认值（参数默认配置值）
     */
    private String parameterDefaultValue;

    /**
     * 内容（监测类型的补充描述）
     */
    private String supplementaryContent;

    /**
     * 字段长度（参数存储的字段长度配置）
     */
    private String fieldLength;


}
