/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzProcessProgress;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzProcessProgress.class, reverseConvertGenerate = false)
public class DzProcessProgressBo extends BaseEntity {

    private Long id;

    private Long defId;

    private Integer status;

    private Date createDate;

    private Integer roundNo;

    private Integer actionType;

    private String description;
}
