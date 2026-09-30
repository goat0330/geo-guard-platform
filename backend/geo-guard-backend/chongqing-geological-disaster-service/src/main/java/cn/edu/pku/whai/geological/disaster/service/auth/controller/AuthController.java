/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.auth.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.domain.model.EmailRegisterBody;
import org.dromara.common.core.domain.model.LoginBody;
import org.dromara.common.core.domain.model.MobileRegisterBody;
import org.dromara.common.core.domain.model.SocialLoginBody;
import org.dromara.common.core.utils.*;
import org.dromara.common.encrypt.annotation.ApiEncrypt;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.mybatis.helper.DataPermissionHelper;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.social.config.properties.SocialLoginConfigProperties;
import org.dromara.common.social.config.properties.SocialProperties;
import org.dromara.common.social.utils.SocialUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import cn.edu.pku.whai.geological.disaster.service.auth.domain.bo.EmailResetBo;
import cn.edu.pku.whai.geological.disaster.service.auth.domain.bo.MobileResetBo;
import cn.edu.pku.whai.geological.disaster.service.auth.domain.vo.LoginTenantVo;
import cn.edu.pku.whai.geological.disaster.service.auth.domain.vo.LoginVo;
import cn.edu.pku.whai.geological.disaster.service.auth.domain.vo.TenantListVo;
import cn.edu.pku.whai.geological.disaster.service.auth.service.IAuthStrategy;
import cn.edu.pku.whai.geological.disaster.service.auth.service.SysLoginService;
import cn.edu.pku.whai.geological.disaster.service.auth.service.SysRegisterService;
import cn.edu.pku.whai.geological.disaster.service.auth.service.SysResetPwdService;
import org.dromara.system.domain.bo.SysTenantBo;
import org.dromara.system.domain.vo.SysClientVo;
import org.dromara.system.domain.vo.SysTenantVo;
import org.dromara.system.service.ISysClientService;
import org.dromara.system.service.ISysSocialService;
import org.dromara.system.service.ISysTenantService;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.kongweiguang.json.Json;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证
 *
 * @author kongweiguang
 */
@Slf4j
@SaIgnore
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final SocialProperties socialProperties;
    private final SysLoginService loginService;
    private final SysRegisterService registerService;
    private final ISysTenantService tenantService;
    private final ISysSocialService socialUserService;
    private final ISysClientService clientService;
    private final SysResetPwdService resetPwdService;


    /**
     * 登录方法
     *
     * @param body 登录信息
     * @return 结果
     */
    @Log(title = "用户登录", businessType = BusinessType.OTHER, isSaveRequestData = false, isSaveResponseData = false, operatorType = OperatorType.PLATFORM)
    @ApiEncrypt
    @PostMapping("/login")
    public R<LoginVo> login(@RequestBody String body) {
        LoginBody loginBody = JsonUtils.parseObject(body, LoginBody.class);
        ValidatorUtils.validate(loginBody);
        // 授权类型和客户端id
        String clientId = loginBody.getClientId();
        String grantType = loginBody.getGrantType();
        SysClientVo client = clientService.queryByClientId(clientId);
        // 查询不到 client 或 client 内不包含 grantType
        if (ObjectUtil.isNull(client) || !StringUtils.contains(client.getGrantType(), grantType)) {
            log.info("客户端id: {} 认证类型：{} 异常!.", clientId, grantType);
            return R.fail(MessageUtils.message("auth.grant.type.error"));
        } else if (!SystemConstants.NORMAL.equals(client.getStatus())) {
            return R.fail(MessageUtils.message("auth.grant.type.blocked"));
        }
        // 校验租户
        loginService.checkTenant(loginBody.getTenantId());
        // 登录
        LoginVo loginVo = IAuthStrategy.login(body, client, grantType);

        return R.ok(loginVo);
    }

    /**
     * 获取跳转URL
     *
     * @param source 登录来源
     * @return 结果
     */
    @GetMapping("/binding/{source}")
    public R<String> authBinding(@PathVariable("source") String source, @RequestParam String domain) {
        SocialLoginConfigProperties obj = socialProperties.getType().get(source);

        if (ObjectUtil.isNull(obj)) {
            return R.fail(source + "平台账号暂不支持");
        }

        AuthRequest authRequest = SocialUtils.getAuthRequest(source, socialProperties);

        Map<String, String> map = new HashMap<>();
        map.put("domain", domain);
        map.put("state", AuthStateUtils.createState());

        String authorizeUrl = authRequest.authorize(Base64.encode(Json.toStr(map), StandardCharsets.UTF_8));
        return R.ok("操作成功", authorizeUrl);
    }

    /**
     * 前端回调绑定授权(需要token)
     *
     * @param loginBody 请求体
     * @return 结果
     */
    @Log(title = "社交账号绑定", businessType = BusinessType.INSERT, isSaveRequestData = false, operatorType = OperatorType.PLATFORM)
    @PostMapping("/social/callback")
    public R<Void> socialCallback(@RequestBody SocialLoginBody loginBody) {
        StpUtil.checkLogin();

        AuthResponse<AuthUser> response = SocialUtils.loginAuth(loginBody.getSource(),
                loginBody.getSocialCode(),
                loginBody.getSocialState(),
                socialProperties);

        AuthUser authUserData = response.getData();

        if (!response.ok()) {
            return R.fail(response.getMsg());
        }

        loginService.socialRegister(authUserData);
        return R.ok();
    }


    /**
     * 取消授权(需要token)
     *
     * @param socialId socialId
     */
    @Log(title = "社交账号解绑", businessType = BusinessType.DELETE, operatorType = OperatorType.PLATFORM)
    @DeleteMapping(value = "/unlock/{socialId}")
    public R<Void> unlockSocial(@PathVariable Long socialId) {
        StpUtil.checkLogin();
        Boolean rows = socialUserService.deleteWithValidById(socialId);
        return rows ? R.ok() : R.fail("取消授权失败");
    }


    /**
     * 退出登录
     */
    @Log(title = "用户退出", businessType = BusinessType.OTHER, isSaveRequestData = false, isSaveResponseData = false, operatorType = OperatorType.PLATFORM)
    @PostMapping("/logout")
    public R<Void> logout() {
        loginService.logout();
        return R.ok("退出成功");
    }

    /**
     * 手机号注册
     */
    @Log(title = "手机号注册", businessType = BusinessType.INSERT, isSaveRequestData = false, isSaveResponseData = false, operatorType = OperatorType.PLATFORM)
    @ApiEncrypt
    @PostMapping("/register/mobile")
    public R<Void> registerMobile(@Validated @RequestBody MobileRegisterBody body) {
        registerService.registerMobile(body);
        return R.ok();
    }

    /**
     * 邮箱注册
     */
    @Log(title = "邮箱注册", businessType = BusinessType.INSERT, isSaveRequestData = false, isSaveResponseData = false, operatorType = OperatorType.PLATFORM)
    @ApiEncrypt
    @PostMapping("/register/email")
    public R<Void> registerEmail(@Validated @RequestBody EmailRegisterBody body) {
        registerService.registerEmail(body);
        return R.ok();
    }

    /**
     * 登录页面租户下拉框
     *
     * @return 租户列表
     */
    @GetMapping("/tenant/list")
    public R<LoginTenantVo> tenantList(HttpServletRequest request) throws Exception {
        // 返回对象
        LoginTenantVo result = new LoginTenantVo();
        boolean enable = TenantHelper.isEnable();
        result.setTenantEnabled(enable);
        // 如果未开启租户这直接返回
        if (!enable) {
            return R.ok(result);
        }

        List<SysTenantVo> tenantList = tenantService.queryList(new SysTenantBo());
        List<TenantListVo> voList = MapstructUtils.convert(tenantList, TenantListVo.class);
        try {
            // 如果只超管返回所有租户
            if (LoginHelper.isSuperAdmin()) {
                result.setVoList(voList);
                return R.ok(result);
            }
        } catch (NotLoginException ignored) {
        }

        // 获取域名
        String host;
        String referer = request.getHeader("referer");
        if (StringUtils.isNotBlank(referer)) {
            // 这里从referer中取值是为了本地使用hosts添加虚拟域名，方便本地环境调试
            host = referer.split("//")[1].split("/")[0];
        } else {
            host = new URL(request.getRequestURL().toString()).getHost();
        }
        // 根据域名进行筛选
        List<TenantListVo> list = StreamUtils.filter(voList, vo ->
                StringUtils.equalsIgnoreCase(vo.getDomain(), host));
        result.setVoList(CollUtil.isNotEmpty(list) ? list : voList);
        return R.ok(result);
    }

    /**
     * 手机号重置密码
     */
    @Log(title = "手机号重置密码", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false, operatorType = OperatorType.PLATFORM)
    @ApiEncrypt
    @PostMapping("/resetPwd/mobile")
    public R<Void> resetPwdMobile(@Validated @RequestBody MobileResetBo bo) {
        DataPermissionHelper.ignore(() -> resetPwdService.resetPwdMobile(bo));
        return R.ok();
    }

    /**
     * 邮箱重置密码
     */
    @Log(title = "邮箱重置密码", businessType = BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false, operatorType = OperatorType.PLATFORM)
    @ApiEncrypt
    @PostMapping("/resetPwd/email")
    public R<Void> resetPwdEmail(@Validated @RequestBody EmailResetBo bo) {
        DataPermissionHelper.ignore(() -> resetPwdService.resetPwdEmail(bo));
        return R.ok();
    }


}
