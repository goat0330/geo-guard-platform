/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzExpertExt;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 专家扩展信息业务对象 dz_expert_ext
 *
 * @author kongweiguang
 * @date 2026-02-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzExpertExt.class, reverseConvertGenerate = false)
public class DzExpertExtBo extends BaseEntity {

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
