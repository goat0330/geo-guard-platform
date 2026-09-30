package cn.edu.pku.whai.geological.disaster.service.service;

import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskDistListRemarkBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskDistListRemarkVo;

import java.util.List;

/**
 * 任务备注 Service
 *
 * @author kongweiguang
 * @date 2026-04-27
 */
public interface IDzTaskDistListRemarkService {

    /**
     * 按任务id查询全部备注（创建时间倒序）
     *
     * @param taskId 任务id
     * @return 备注列表
     */
    List<DzTaskDistListRemarkVo> queryListByTaskId(Long taskId);

    /**
     * 新增备注
     *
     * @param bo 新增参数
     * @return 是否成功
     */
    Boolean insertByBo(DzTaskDistListRemarkBo bo);

}
