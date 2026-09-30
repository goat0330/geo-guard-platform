/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

/**
 * 三方监测预警 SSO 凭证失效后用于终止当前批次后续请求。
 */
class ThirdPartyWarningAuthException extends RuntimeException {

    ThirdPartyWarningAuthException(String message) {
        super(message);
    }

    ThirdPartyWarningAuthException(String message, Throwable cause) {
        super(message);
        if (cause != null) {
            initCause(cause);
        }
    }
}
