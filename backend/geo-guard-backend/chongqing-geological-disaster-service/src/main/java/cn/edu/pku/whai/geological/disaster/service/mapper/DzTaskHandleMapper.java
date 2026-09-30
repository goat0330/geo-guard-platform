package cn.edu.pku.whai.geological.disaster.service.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleVo;

import java.util.List;

/**
 * 灾害处置Mapper接口
 *
 * @author kongweiguang
 * @date 2026-01-29
 */
public interface DzTaskHandleMapper extends BaseMapperPlus<DzTaskHandle, DzTaskHandleVo> {

    List<DzTaskHandleVo> getAllHandling();
}
