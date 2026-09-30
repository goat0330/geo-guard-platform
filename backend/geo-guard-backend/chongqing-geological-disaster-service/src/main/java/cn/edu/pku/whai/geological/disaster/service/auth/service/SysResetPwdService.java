/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.auth.service;

import cn.dev33.satoken.secure.BCrypt;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.exception.user.CaptchaExpireException;
import org.dromara.common.core.exception.user.UserException;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import cn.edu.pku.whai.geological.disaster.service.auth.domain.bo.EmailResetBo;
import cn.edu.pku.whai.geological.disaster.service.auth.domain.bo.MobileResetBo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysUserService;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class SysResetPwdService {
    private final ISysUserService sysUserService;
    private final SysLoginService loginService;

    private boolean validateCode(String tenantId, String identity, String userCode) {
        String code = RedisUtils.getCacheObject(GlobalConstants.CAPTCHA_CODE_KEY + identity);
        if (StringUtils.isBlank(code)) {
            loginService.recordLogininfor(tenantId, identity, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        return code.equals(userCode);
    }

    public void resetPwdMobile(MobileResetBo bo) {
        String phonenumber = bo.getPhonenumber();
        String smsCode = bo.getSmsCode();
        String password = bo.getPassword();
        boolean valid = validateCode(null, phonenumber, smsCode);
        if (!valid) {
            throw new CaptchaExpireException();
        }
        SysUserVo sysUserVo = sysUserService.selectUserByPhonenumber(phonenumber);
        if (ObjectUtil.isNull(sysUserVo)) {
            throw new UserException("user.not.exists");
        }
        sysUserService.resetUserPwd(sysUserVo.getUserId(), BCrypt.hashpw(password));

    }

    public void resetPwdEmail(EmailResetBo bo) {
        String email = bo.getEmail();
        String emailCode = bo.getEmailCode();
        String password = bo.getPassword();
        boolean valid = validateCode(null, email, emailCode);
        if (!valid) {
            throw new CaptchaExpireException();
        }
        SysUserVo sysUserVo = sysUserService.selectUserByEmail(email);
        if (ObjectUtil.isNull(sysUserVo)) {
            throw new UserException("user.not.exists");
        }

        sysUserService.resetUserPwd(sysUserVo.getUserId(), BCrypt.hashpw(password));
    }
}
