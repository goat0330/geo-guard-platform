/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import org.dromara.system.domain.vo.SysUserVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 地灾项目用户视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DzUserVo extends SysUserVo {

    /**
     * 用户绑定的行政区划列表
     */
    private List<DzUserAdRegionVo> userAdRegions;
}
