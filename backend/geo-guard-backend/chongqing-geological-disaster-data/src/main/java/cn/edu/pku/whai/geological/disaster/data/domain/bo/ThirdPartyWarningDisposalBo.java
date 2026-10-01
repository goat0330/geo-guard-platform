/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 三方预警处置查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ThirdPartyWarningDisposalBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 监测点名称
     */
    private String monitorPointName;

    /**
     * 行政区划编码
     */
    private String regionCode;

    /**
     * 预警发布时间
     */
    private String warningPublishTime;

    /**
     * 预警处置时间
     */
    private String warningDisposalTime;

    /**
     * 预警等级: 1-4
     */
    private Integer warningLevel;

    /**
     * 是否有效预警: 1-有效, 0-无效
     */
    private Integer validWarning;

    /**
     * 处置状态: 1-已处置, 0-未处置
     */
    private Integer disposalStatus;

    /**
     * 处置类型: 0.数据异常导致误报, 1.设备维护导致误报, 2.设备遭到破坏, 3.正常预警, 4.预警模型待优化
     */
    private String disposalType;
}
