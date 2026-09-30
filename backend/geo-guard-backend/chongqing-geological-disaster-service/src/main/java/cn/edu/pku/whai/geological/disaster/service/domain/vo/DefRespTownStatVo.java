/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 乡镇级防御响应统计。
 */
@Data
public class DefRespTownStatVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前全部流转中、未关闭、未删除的乡镇级防御响应汇总。
     */
    private DefRespTownStatItemVo total;

    /**
     * 按响应等级汇总。
     */
    private List<DefRespTownStatItemVo> levelStats = new ArrayList<>();
}
