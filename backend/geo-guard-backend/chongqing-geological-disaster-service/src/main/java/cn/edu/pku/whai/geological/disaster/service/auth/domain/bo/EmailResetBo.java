/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.auth.domain.bo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailResetBo {
    /**
     * 邮箱
     */
    @NotBlank(message = "{user.email.not.blank}")
    @Email(message = "{user.email.not.valid}")
    private String email;

    /**
     * 邮箱code
     */
    @NotBlank(message = "{email.code.not.blank}")
    private String emailCode;


    @NotBlank(message = "密码不能为空")
    private String password;
}
