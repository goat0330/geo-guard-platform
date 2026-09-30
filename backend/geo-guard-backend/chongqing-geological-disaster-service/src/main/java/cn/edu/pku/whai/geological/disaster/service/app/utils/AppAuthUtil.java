/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.app.utils;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.SpringUtils;
import cn.edu.pku.whai.geological.disaster.service.app.props.AppTaskProps;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

public class AppAuthUtil {
    public static void isAuth() {
        String token = getToken();

        if (token == null) {
            throw new ServiceException("用户未授权，无法调用此接口");
        }

        if (!StrUtil.equals(token, SpringUtils.getBean(AppTaskProps.class).getSysToken())) {
            throw new ServiceException("用户未授权，无法调用此接口");
        }
    }

    public static String getToken() {
        HttpServletRequest request = ServletUtils.getRequest();
        String header = request.getHeader("Authorization");

        if (header == null) {
            return null;
        }

        header = StrUtil.removePrefix(header, "Bearer ");
        return header;
    }

}
