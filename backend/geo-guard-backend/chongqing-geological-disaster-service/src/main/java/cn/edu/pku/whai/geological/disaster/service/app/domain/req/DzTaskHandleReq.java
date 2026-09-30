package cn.edu.pku.whai.geological.disaster.service.app.domain.req;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 灾害处置业务对象 dz_task_handle
 *
 * @author kongweiguang
 * @date 2026-01-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzTaskHandle.class, reverseConvertGenerate = false)
public class DzTaskHandleReq extends BaseEntity {

    /**
     * 处置方案ID
     */
    private Long handleId;
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
     * 上报人
     */
    private String reporter;

    /**
     * 上报时间
     */
    private Date reporterDate;

}
