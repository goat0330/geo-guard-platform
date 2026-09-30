/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.system.domain.bo.SysUserBo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 地灾项目用户业务对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DzUserBo extends SysUserBo {

    /**
     * 用户绑定的行政区划列表
     */
    private List<DzUserAdRegionBo> userAdRegions;
}
