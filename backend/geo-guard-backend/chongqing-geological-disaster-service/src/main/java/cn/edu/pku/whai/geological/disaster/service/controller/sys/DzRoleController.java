/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller.sys;

import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.anno.ReqLogIgnore;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRoleUserBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.RoleUserVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.StatRoleCountVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzRoleService;
import org.dromara.system.domain.vo.SysRoleVo;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色相关
 *
 * @author kongweiguang
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/role")
public class DzRoleController {
    private final IDzRoleService iDzRoleService;

    /**
     * 统计不同角色的数量
     *
     * @return
     */
    @PostMapping("stat")
    public R<List<StatRoleCountVo>> stat() {
        List<StatRoleCountVo> vo = iDzRoleService.statCount();
        return R.ok(vo);
    }

    /**
     * 查询角色用户列表
     *
     * @return
     */
    @ReqLogIgnore
    @GetMapping("expert")
    public TableDataInfo<RoleUserVo> expert(DzRoleUserBo bo, PageQuery pageQuery) {
        return iDzRoleService.queryPageList(bo, pageQuery);
    }

    /**
     * 根据用户ID查询角色
     */
    @GetMapping("{userId}")
    public R<List<SysRoleVo>> getRolesByUserId(@NotNull(message = "用户ID不能为空")
                                               @PathVariable Long userId) {
        return R.ok(iDzRoleService.queryRolesByUserId(userId));
    }

}
