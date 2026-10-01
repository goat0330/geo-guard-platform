/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 监测设备树形结构响应类
 *
 * @author lizheng
 * @date 2026-01-21
 */
@Data
public class MonitorDeviceTreeResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 节点ID
     */
    private String id;

    /**
     * 父节点ID
     */
    @JsonProperty("parentId")
    private String parentId;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 对象（设备节点时为MonitorDeviceObjectResp，传感器节点时为MonitorSensorObjectResp）
     */
    private Object object;

    /**
     * 子节点列表
     */
    private List<MonitorDeviceTreeResp> children;
}
