/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzDefRespAlarmHistory;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzDefRespAlarmHistory.class, reverseConvertGenerate = false)
public class DzDefRespAlarmHistoryBo extends BaseEntity {

    private Long id;

    private Long alarmId;

    private Long defId;

    private String alarmCode;

    private Date triggerTime;

    private Integer defRespLevel;

    private Integer alarmLevel;

    private String alarmSource;

    private Integer alarmSourceType;

    private Date createDate;
}
