/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.sse.domain.resp;

import org.dromara.common.json.utils.JsonUtils;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SseResp {
    private String id;
    private String type;
    private Object data;

    public String toStr() {
        return JsonUtils.toJsonString(this);
    }
}
