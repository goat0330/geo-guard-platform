/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.auth.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.domain.model.SocialLoginBody;
import org.dromara.common.core.enums.UserType;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.exception.user.UserException;
import org.dromara.common.core.utils.ValidatorUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.social.config.properties.SocialProperties;
import org.dromara.common.social.utils.SocialUtils;
import cn.edu.pku.whai.geological.disaster.service.auth.domain.vo.LoginVo;
import cn.edu.pku.whai.geological.disaster.service.auth.service.IAuthStrategy;
import cn.edu.pku.whai.geological.disaster.service.auth.service.SysLoginService;
import cn.edu.pku.whai.geological.disaster.service.auth.service.SysRegisterService;
import org.dromara.system.domain.bo.SysSocialBo;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysClientVo;
import org.dromara.system.domain.vo.SysSocialVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysSocialService;
import org.dromara.system.service.ISysUserService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 第三方授权策略
 *
 * @author thiszhc is 三三
 */
@Slf4j
@Service("social" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class SocialAuthStrategy implements IAuthStrategy {

    private final SocialProperties socialProperties;
    private final ISysSocialService sysSocialService;
    private final SysUserMapper userMapper;
    private final SysLoginService loginService;
    private final ISysUserService userService;
    private final SysRegisterService registerService;

    /**
     * 登录-第三方授权登录
     *
     * @param body   登录信息
     * @param client 客户端信息
     */
    @Override
    public LoginVo login(String body, SysClientVo client) {
        SocialLoginBody loginBody = JsonUtils.parseObject(body, SocialLoginBody.class);
        ValidatorUtils.validate(loginBody);

        AuthResponse<AuthUser> response = SocialUtils.loginAuth(
                loginBody.getSource(), loginBody.getSocialCode(),
                loginBody.getSocialState(), socialProperties);

        if (!response.ok()) {
            throw new ServiceException(response.getMsg());
        }

        AuthUser authUserData = response.getData();
        String authId = authUserData.getSource() + authUserData.getUuid();

        List<SysSocialVo> list = sysSocialService.selectByAuthId(authId);

        Long userId;
        if (CollUtil.isEmpty(list)) {
            SysUserBo sysUser = new SysUserBo();
            sysUser.setPassword(BCrypt.hashpw(IdUtil.fastSimpleUUID()));
            sysUser.setUserType(UserType.SYS_USER.getUserType());
            boolean regFlag = registerService.registerUser(sysUser);
            if (!regFlag) {
                throw new UserException("user.register.error");
            }

            SysSocialBo bo = BeanUtil.toBean(authUserData, SysSocialBo.class);
            BeanUtil.copyProperties(authUserData.getToken(), bo);
            bo.setUserId(sysUser.getUserId());
            bo.setAuthId(authId);
            bo.setOpenId(authUserData.getUuid());
            bo.setUserName(authUserData.getUsername());
            bo.setNickName(authUserData.getNickname());
            sysSocialService.insertByBo(bo);

            userId = sysUser.getUserId();
        } else {
            SysSocialVo social = list.get(0);
            userId = social.getUserId();
        }

        SysUserVo sysUserVo = loadUser(userId);

        LoginUser loginUser = loginService.buildLoginUser(sysUserVo);

        loginUser.setClientKey(client.getClientKey());
        loginUser.setDeviceType(client.getDeviceType());

        SaLoginModel model = new SaLoginModel();
        model.setDevice(client.getDeviceType());
        model.setTimeout(client.getTimeout());
        model.setActiveTimeout(client.getActiveTimeout());
        model.setExtra(LoginHelper.CLIENT_KEY, client.getClientId());

        LoginHelper.login(loginUser, model);

        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(StpUtil.getTokenValue());
        loginVo.setExpireIn(StpUtil.getTokenTimeout());
        loginVo.setClientId(client.getClientId());
        return loginVo;
    }

    private SysUserVo loadUser(Long userId) {
        SysUserVo user = userMapper.selectVoById(userId);
        if (ObjectUtil.isNull(user)) {
            log.info("登录用户：{} 不存在.", "");
            throw new UserException("user.not.exists", "");
        } else if (SystemConstants.DISABLE.equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", "");
            throw new UserException("user.blocked", "");
        }
        return user;
    }

}
