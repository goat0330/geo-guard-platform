/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzReportDisaster;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.Map;

/**
 * 报灾查询条件。
 *
 * @author kongweiguang
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzReportDisaster.class, reverseConvertGenerate = false)
public class DzReportDisasterBo extends BaseEntity {

    private Long id;
    private Long userId;
    private String userName;
    private String userPhone;
    private String userRole;
    private Date checkTime;
    private String checkCenter;
    private String checkCenterLocation;
    private String detailedAddress;
    private String photos;
    private String sceneTextRecord;
    private Integer aiRiskLevel;
    private String aiRiskLabel;
    private String aiReportDetail;
    private Map<String, Object> aiVisionProps;
    private Integer manualRiskLevel;
    private String manualRiskRemark;
    private Integer status;
    private Integer sourceType;
    private Long taskId;
    private Date createDate;
    private Date updateDate;
}
