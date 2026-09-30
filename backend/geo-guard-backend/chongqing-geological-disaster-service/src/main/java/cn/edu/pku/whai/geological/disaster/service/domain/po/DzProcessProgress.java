/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("dz_process_progress")
public class DzProcessProgress implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 防御响应方案id
     */
    private Long defId;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 所属轮次，对应 dz_def_resp_plan.current_round_no。
     */
    private Integer roundNo;

    /**
     * 进度动作类型：1正向流转 2回退标记 3预警自动重置。
     */
    private Integer actionType;

    /**
     * 进度描述，写入时直接落库。
     */
    private String description;
}
