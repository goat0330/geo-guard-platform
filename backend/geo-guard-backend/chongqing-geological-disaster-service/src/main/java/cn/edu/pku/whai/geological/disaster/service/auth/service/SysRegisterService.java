/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.auth.service;

import cn.dev33.satoken.secure.BCrypt;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.domain.model.EmailRegisterBody;
import org.dromara.common.core.domain.model.MobileRegisterBody;
import org.dromara.common.core.enums.UserType;
import org.dromara.common.core.exception.user.CaptchaExpireException;
import org.dromara.common.core.exception.user.UserException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import cn.edu.pku.whai.geological.disaster.service.auth.props.AuthCenterProps;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysRegisterService {

    private final SysUserMapper userMapper;
    private final SysLoginService loginService;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final AuthCenterProps authConfig;

    private void validateCode(String tenantId, String identity, String userCode) {
        String code = RedisUtils.getCacheObject(GlobalConstants.CAPTCHA_CODE_KEY + identity);
        if (StringUtils.isBlank(code)) {
            loginService.recordLogininfor(tenantId, identity, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }

        if (!code.equals(userCode)) {
            throw new UserException("user.jcaptcha.error");
        }
    }

    public void registerMobile(MobileRegisterBody body) {
        String userType = UserType.getUserType(body.getUserType()).getUserType();
        String username = body.getUsername();
        String password = body.getPassword();

        String phonenumber = body.getPhonenumber();
        String smsCode = body.getSmsCode();

        validateCode(null, phonenumber, smsCode);

        boolean exist = userMapper.exists(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getPhonenumber, phonenumber));

        if (exist) {
            throw new UserException("user.register.save.error", phonenumber);
        }

        SysUserBo sysUser = new SysUserBo();
        sysUser.setUserName(username);
        sysUser.setPassword(BCrypt.hashpw(password));
        sysUser.setUserType(userType);
        sysUser.setPhonenumber(phonenumber);
        sysUser.setNickName(body.getNickName());
        sysUser.setOrgName(body.getOrgName());

        boolean regFlag = registerUser(sysUser);
        if (!regFlag) {
            throw new UserException("user.register.error");
        }

        loginService.recordLogininfor(null, username, Constants.REGISTER, MessageUtils.message("user.register.success"));
    }

    public void registerEmail(EmailRegisterBody body) {
        String userType = UserType.getUserType(body.getUserType()).getUserType();
        String username = body.getUsername();
        String password = body.getPassword();

        String email = body.getEmail();
        String smsCode = body.getEmailCode();

        validateCode(null, email, smsCode);

        boolean exist = userMapper.exists(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getEmail, email));

        if (exist) {
            throw new UserException("user.register.save.error", email);
        }

        SysUserBo sysUser = new SysUserBo();
        sysUser.setUserName(username);
        sysUser.setPassword(BCrypt.hashpw(password));
        sysUser.setUserType(userType);
        sysUser.setEmail(email);
        sysUser.setNickName(body.getNickName());
        sysUser.setOrgName(body.getOrgName());

        boolean regFlag = registerUser(sysUser);
        if (!regFlag) {
            throw new UserException("user.register.error");
        }

        loginService.recordLogininfor(null, username, Constants.REGISTER, MessageUtils.message("user.register.success"));
    }


    public boolean registerUser(SysUserBo user) {
        try {
            user.setCreateBy(0L);
            user.setUpdateBy(0L);
            SysUser sysUser = MapstructUtils.convert(user, SysUser.class);
            int insertUser = userMapper.insert(sysUser);
            if (insertUser <= 0) {
                return false;
            }

            user.setUserId(sysUser.getUserId());
            LambdaQueryWrapper<SysRole> lqw = Wrappers.lambdaQuery();
            lqw.in(SysRole::getRoleKey, authConfig.getDefaultPerms());
            List<SysRole> sysRoles = roleMapper.selectList(lqw);
            for (SysRole sysRole : sysRoles) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(sysUser.getUserId());
                ur.setRoleId(sysRole.getRoleId());
                int insertRole = userRoleMapper.insert(ur);
                if (insertRole <= 0) {
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("saveUser error. ", e);
            return false;
        }

        return true;
    }
}
