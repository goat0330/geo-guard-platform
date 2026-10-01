/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.resp.warning;

import lombok.Data;

/**
 * 第三方气象风险预警预警接口通用返回类
 */
@Data
public class ThirdPartyWarningDataResp {
    private String status;
    private String message;
    private Object data;

    public Boolean isSuccess() {
        return "200".equals(this.status);
    }
}
