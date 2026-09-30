/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * 灾害处置查询条件。
 *
 * @author kongweiguang
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzTaskHandle.class, reverseConvertGenerate = false)
public class DzTaskHandleBo extends BaseEntity {

    @NotNull(message = "id不能为空", groups = {EditGroup.class})
    private Long id;
    private String province;
    private String city;
    private String county;
    private String street;
    private String village;
    private String center;
    private String detailedAddress;
    private Integer eventType;
    private Integer eventLevel;
    private Integer respStatus;
    private Integer handleProcess;
    private List<Integer> handleProcessList;
    private String influenceScope;
    private String responsiblePerson;
    private String responsiblePersonPhone;
    private String responsiblePersonRole;
    private String reporter;
    private Date reporterDate;
    private String slopeUnitId;
    private Integer peopleLeave;
    private Integer isSkipped;
    private Date createDate;
    private Date updateDate;
}
