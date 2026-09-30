/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.auth.domain.to;

import cn.dev33.satoken.stp.SaLoginModel;
import org.dromara.common.core.domain.model.LoginUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginTicketTo {
    private LoginUser loginUser;
    private SaLoginModel saLoginModel;
}
