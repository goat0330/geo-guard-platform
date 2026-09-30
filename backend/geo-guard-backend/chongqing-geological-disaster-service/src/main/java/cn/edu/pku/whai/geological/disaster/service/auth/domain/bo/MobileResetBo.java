/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.auth.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MobileResetBo {
    /**
     * 手机号
     */
    @NotBlank(message = "{user.phonenumber.not.blank}")
    private String phonenumber;

    /**
     * 短信code
     */
    @NotBlank(message = "{sms.code.not.blank}")
    private String smsCode;

    @NotBlank(message = "密码不能为空")
    private String password;
}
