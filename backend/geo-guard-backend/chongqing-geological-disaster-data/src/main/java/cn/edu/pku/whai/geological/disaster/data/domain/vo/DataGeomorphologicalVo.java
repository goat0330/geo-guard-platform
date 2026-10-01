/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import cn.edu.pku.whai.geological.disaster.data.domain.po.DataGeomorphological;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 地貌信息VO
 *
 * @author zhuzc
 * @date 2026-06-11
 */
@Data
@AutoMapper(target = DataGeomorphological.class)
public class DataGeomorphologicalVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 几何WKT
     */
    private String wkt;

    /**
     * 地貌类型
     */
    private String type;

    /**
     * 地貌特征
     */
    private String feature;
}
