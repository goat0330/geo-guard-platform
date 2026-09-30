/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用业务缓存请求参数
 */
@Data
public class DzBusinessCacheBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 缓存范围：0-用户独有，1-全局
     */
    private Integer scope;

    @NotBlank(message = "业务类型不能为空")
    private String businessType;

//    @NotBlank(message = "业务内容不能为空")
    private String businessContent;
}
