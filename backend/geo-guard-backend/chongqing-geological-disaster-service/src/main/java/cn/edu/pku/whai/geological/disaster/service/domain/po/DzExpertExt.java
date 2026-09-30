package cn.edu.pku.whai.geological.disaster.service.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 专家扩展信息对象 dz_expert_ext
 *
 * @author kongweiguang
 * @date 2026-02-03
 */
@Data
@TableName("dz_expert_ext")
public class DzExpertExt implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 专家类型（1：地质专家，2：水文专家）
     */
    private Integer expertType;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 会商次数
     */
    private Integer meetingCount;


}
