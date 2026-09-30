/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.ThirdPartyWarningDataResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.UserInfoResp;
import cn.edu.pku.whai.geological.disaster.data.props.WarningDataProps;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.kongweiguang.http.client.Req;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ThirdPartyWarningRemoteSupport {

    private final WarningDataProps warningDataProps;

    private volatile String token;

    public synchronized UserInfoResp loginAuth() {
        String userName = warningDataProps.getUserName();
        if (StrUtil.isBlank(userName)) {
            throw new ServiceException("用户名不能为空,请配置用户名");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("userName", userName);
        ThirdPartyWarningDataResp resp = Req.post(warningDataProps.getUrl())
                                            .path("/api/dz/cas/pub/loginAuth")
                                            .json(params)
                                            .timeout(Duration.ofMinutes(3))
                                            .ok()
                                            .obj(ThirdPartyWarningDataResp.class);
        if (ObjectUtil.isNull(resp) || !resp.isSuccess() || ObjectUtil.isNull(resp.getData())) {
            return null;
        }

        UserInfoResp result = JSONUtil.toBean(JSONUtil.parseObj(resp.getData()), UserInfoResp.class);
        token = result == null ? null : result.getCredential();
        return result;
    }

    public void ensureWarningSsoToken() {
        if (StrUtil.isBlank(token)) {
            loginAuth();
        }
        if (StrUtil.isBlank(token)) {
            throw new ServiceException("三方接口登录失败，未获取到sso凭证");
        }
    }

    public String getToken() {
        ensureWarningSsoToken();
        return token;
    }

    public String getRawToken() {
        return token;
    }

    public String getMonitorWarningUrl() {
        return StrUtil.blankToDefault(warningDataProps.getMonitorUrl(), warningDataProps.getUrl());
    }

    public ServiceException buildThirdPartyException(String prefix, ThirdPartyWarningDataResp resp) {
        if (resp == null) {
            return new ServiceException(prefix + ": 三方接口无响应");
        }
        String status = StrUtil.nullToDefault(resp.getStatus(), "");
        String message = StrUtil.blankToDefault(resp.getMessage(), "三方接口返回失败");
        return new ServiceException(prefix + ": " + message + (StrUtil.isBlank(status) ? "" : " [status=" + status + "]"));
    }
}
