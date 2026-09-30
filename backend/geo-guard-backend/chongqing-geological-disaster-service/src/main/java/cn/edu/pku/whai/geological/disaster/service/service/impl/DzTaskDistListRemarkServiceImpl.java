package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskDistListRemarkBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistListRemark;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskDistListRemarkVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListRemarkMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListRemarkService;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 任务备注 Service 实现
 *
 * @author kongweiguang
 * @date 2026-04-27
 */
@RequiredArgsConstructor
@Service
public class DzTaskDistListRemarkServiceImpl implements IDzTaskDistListRemarkService {

    private final DzTaskDistListRemarkMapper baseMapper;
    private final DzTaskDistListMapper dzTaskDistListMapper;

    @Override
    public List<DzTaskDistListRemarkVo> queryListByTaskId(Long taskId) {
        List<DzTaskDistListRemark> remarks = baseMapper.selectListByTaskId(taskId);
        return MapstructUtils.convert(remarks, DzTaskDistListRemarkVo.class);
    }

    @Override
    public Boolean insertByBo(DzTaskDistListRemarkBo bo) {
        if (bo == null || bo.getTaskId() == null) {
            throw new ServiceException("任务id不能为空");
        }
        if (dzTaskDistListMapper.selectById(bo.getTaskId()) == null) {
            throw new ServiceException("任务不存在，无法新增备注");
        }
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("当前用户未登录或登录已失效");
        }

        DzTaskDistListRemark entity = MapstructUtils.convert(bo, DzTaskDistListRemark.class);
        entity.setId(IdUtil.getSnowflakeNextId());
        entity.setUserId(userId);
        entity.setCreateDate(new Date());
        entity.setUpdateDate(new Date());
        return baseMapper.insertEntity(entity) > 0;
    }
}
