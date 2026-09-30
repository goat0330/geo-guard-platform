/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.app.domain.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 巡查任务催办实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionRemindReq implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 任务唯一ID
     */
    private Long taskId;
}