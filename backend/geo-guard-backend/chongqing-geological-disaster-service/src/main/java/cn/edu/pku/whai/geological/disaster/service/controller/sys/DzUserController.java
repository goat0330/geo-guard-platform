/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller.sys;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.secure.BCrypt;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzUserBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserService;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.service.ISysDeptService;
import org.dromara.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * dz用户管理
 *
 * @author kongweiguang
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/user")
public class DzUserController extends BaseController {

    private final IDzUserService dzUserService;
    private final ISysDeptService deptService;
    private final ISysUserService userService;

    /**
     * 查询用户列表
     */
    @GetMapping("/list")
    public TableDataInfo<DzUserVo> list(SysUserBo user, PageQuery pageQuery) {
        return dzUserService.selectPageUserList(user, pageQuery);
    }

    /**
     * 新增用户
     */
    @SaCheckPermission("system:user:add")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated @RequestBody DzUserBo user) {
        deptService.checkDeptDataScope(user.getDeptId());
        if (!userService.checkUserNameUnique(user)) {
            return R.fail("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        } else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user)) {
            return R.fail("新增用户'" + user.getUserName() + "'失败，手机号码已存在");
        } else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user)) {
            return R.fail("新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }

        String password = user.getPassword();
        if (StringUtils.isEmpty(password)) {
            password = SystemConstants.DEFAULT_PASSWORD;
        }
        user.setPassword(BCrypt.hashpw(password));
        return R.ok(dzUserService.insertUser(user));
    }

    /**
     * 修改用户
     */
    @SaCheckPermission("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody DzUserBo user) {
        userService.checkUserAllowed(user.getUserId());
        userService.checkUserDataScope(user.getUserId());
        deptService.checkDeptDataScope(user.getDeptId());
        if (!userService.checkUserNameUnique(user)) {
            return R.fail("修改用户'" + user.getUserName() + "'失败，登录账号已存在");
        } else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user)) {
            return R.fail("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        } else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user)) {
            return R.fail("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        return toAjax(dzUserService.updateUser(user));
    }
}
