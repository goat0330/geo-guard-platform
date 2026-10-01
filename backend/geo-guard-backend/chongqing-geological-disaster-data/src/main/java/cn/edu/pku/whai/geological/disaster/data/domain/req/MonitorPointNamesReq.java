/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 监测点名称批量查询请求对象
 */
@Data
public class MonitorPointNamesReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 监测点名称列表
     */
    private List<String> monitorPointNames;
}
