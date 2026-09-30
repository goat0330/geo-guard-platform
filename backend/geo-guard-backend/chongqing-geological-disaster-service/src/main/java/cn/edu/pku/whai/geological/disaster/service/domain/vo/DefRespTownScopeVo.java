/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 当前有效乡镇级防御响应范围。
 */
@Data
public class DefRespTownScopeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer level;

    private Integer status;

    private Integer executeStatus;

    private Date createDate;

    private Date updateDate;

    private String county;

    private String street;
}
