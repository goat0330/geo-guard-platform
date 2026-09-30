package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 灾害处置视图对象 dz_task_handle
 *
 * @author kongweiguang
 * @date 2026-01-29
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DzTaskHandle.class)
public class DzTaskHandleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @ExcelProperty(value = "id")
    private Long id;

    /**
     * 所属省份
     */
    @ExcelProperty(value = "所属省份")
    private String province;

    /**
     * 所属地级市
     */
    @ExcelProperty(value = "所属地级市")
    private String city;

    /**
     * 所属区/县
     */
    @ExcelProperty(value = "所属区/县")
    private String county;

    /**
     * 所属乡镇/街道
     */
    @ExcelProperty(value = "所属乡镇/街道")
    private String street;

    /**
     * 所属行政村
     */
    @ExcelProperty(value = "所属行政村")
    private String village;

    /**
     * 中心点坐标
     */
    @ExcelProperty(value = "中心点坐标")
    private String center;

    /**
     * 详细地址
     */
    @ExcelProperty(value = "详细地址")
    private String detailedAddress;

    /**
     * 事件类型（1：滑坡 2：崩塌 3：地面塌陷 4：泥石流 5：危岩  100：其他）
     */
    @ExcelProperty(value = "事件类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1=滑坡,2=崩塌,3=地面塌陷,4=泥石流,5=危岩,100=其他")
    private Integer eventType;

    /**
     * 事件级别（内部编码统一为 1：小型 2：中型 3：大型 4：特大型；导出时转换为业务文案）
     */
    @ExcelProperty(value = "事件级别", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1=小型,2=中型,3=大型,4=特大型")
    private Integer eventLevel;

    /**
     * 响应状态（内部编码统一为 0：未启动响应 1：4级响应 2：3级响应 3：2级响应 4：1级响应；导出时转换为业务文案）
     */
    @ExcelProperty(value = "响应状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=未启动响应,1=4级响应,2=3级响应,3=2级响应,4=1级响应")
    private Integer respStatus;

    /**
     * 处理进度（1：应急调查 2：会商研判 3：方案接入 4：响应执行 5：闭环归档）
     */
    @ExcelProperty(value = "处理进度", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1=应急调查,2=会商研判,3=方案接入,4=响应执行,5=闭环归档")
    private Integer handleProcess;

    /**
     * 影响范围
     */
    @ExcelProperty(value = "影响范围")
    private String influenceScope;

    /**
     * 负责人
     */
    @ExcelProperty(value = "负责人")
    private String responsiblePerson;

    /**
     * 负责人手机号
     */
    @ExcelProperty(value = "负责人手机号")
    private String responsiblePersonPhone;

    /**
     * 负责人角色
     */
    @ExcelProperty(value = "负责人角色")
    private String responsiblePersonRole;

    /**
     * 村支书
     */
    @ExcelProperty(value = "村支书")
    private String villageSecretary;

    /**
     * 村支书电话号码
     */
    @ExcelProperty(value = "村支书电话号码")
    private String villageSecretaryPhone;

    /**
     * 上报人
     */
    @ExcelProperty(value = "上报人")
    private String reporter;

    /**
     * 上报时间
     */
    @ExcelProperty(value = "上报时间")
    private Date reporterDate;

    /**
     * 斜坡单元id
     */
    @ExcelProperty(value = "斜坡单元id")
    private String slopeUnitId;

    /**
     * 群众撤离（0：不涉及，1：涉及）
     */
    @ExcelProperty(value = "群众撤离", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=不涉及，1=涉及")
    private Integer peopleLeave;

    /**
     * 是否跳过: 0.否 1.是
     */
    @ExcelProperty(value = "是否跳过", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=否,1=是")
    private Integer isSkipped;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createDate;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateDate;

    /**
     * 危险范围
     */
    @ExcelProperty(value = "危险范围")
    private String hazardExtent;

    /**
     * 风险范围
     */
    @ExcelProperty(value = "风险范围")
    private String riskExtent;

}
