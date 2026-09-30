/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 启动短信预览结果
 */
@Data
public class DefRespStartSmsPreviewVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String bizKey;

    private String bizName;

    private Long userId;

    private String nickName;

    private String phone;

    /**
     * 命中的最高行政区层级
     */
    private Integer adRegionLevel;

    /**
     * 参与聚合的行政区ID列表
     */
    private List<String> adRegionIds;

    /**
     * 参与聚合的行政区名称列表
     */
    private List<String> adRegionNames;

    /**
     * 参与渲染的乡镇名称列表
     */
    private List<String> streetNames;

    /**
     * 参与渲染的村名称列表
     */
    private List<String> villageNames;

    /**
     * 短信内容
     */
    private String smsContent;
}
