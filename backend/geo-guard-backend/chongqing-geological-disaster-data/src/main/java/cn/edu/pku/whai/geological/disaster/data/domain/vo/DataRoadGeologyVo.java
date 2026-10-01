/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.DataRoadGeology;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 道路地质信息VO
 *
 * @author zhuzc
 * @date 2026-06-04
 */
@Data
@AutoMapper(target = DataRoadGeology.class)
public class DataRoadGeologyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private String id;

    /** 道路名称 */
    private String roadName;
}
