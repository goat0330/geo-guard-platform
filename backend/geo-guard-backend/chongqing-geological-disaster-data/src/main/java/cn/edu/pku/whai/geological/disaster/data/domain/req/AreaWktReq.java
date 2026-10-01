/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 范围WKT查询请求对象
 *
 * @author system
 * @date 2026-05-15
 */
@Data
public class AreaWktReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 范围WKT
     */
    @NotBlank(message = "wkt不能为空")
    private String wkt;
}
