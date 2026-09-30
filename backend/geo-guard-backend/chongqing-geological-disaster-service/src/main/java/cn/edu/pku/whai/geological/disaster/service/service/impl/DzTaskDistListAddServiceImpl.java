package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.SysRoleEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskDistListAddBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistListAdd;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskDistListAddVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListAddMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListAddService;
import cn.edu.pku.whai.geological.disaster.service.utils.CurrentRoleUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 任务上报信息 Service 实现
 *
 * @author kongweiguang
 * @date 2026-04-15
 */
@RequiredArgsConstructor
@Service
public class DzTaskDistListAddServiceImpl implements IDzTaskDistListAddService {

    private final DzTaskDistListAddMapper baseMapper;

    @Override
    public DzTaskDistListAddVo queryByKeys(Long taskId, Long userId) {
        return baseMapper.selectVoByKeys(taskId, userId);
    }

    @Override
    public TableDataInfo<DzTaskDistListAddVo> queryPageList(DzTaskDistListAddBo bo, PageQuery pageQuery) {
        Page<DzTaskDistListAddVo> result = baseMapper.selectVoPage(pageQuery.build(), bo);
        return TableDataInfo.build(result);
    }

    @Override
    public List<DzTaskDistListAddVo> queryList(DzTaskDistListAddBo bo) {
        return baseMapper.selectVoList(bo);
    }

    @Override
    public Boolean insertByBo(DzTaskDistListAddBo bo) {
        if (!CurrentRoleUtil.hasAnyRole(
            SysRoleEnum.DZ_FGXIANZHANG.getRoleKey(),
            SysRoleEnum.DZ_ZGJLD.getRoleKey(),
            SysRoleEnum.DZ_FGXIANGZHANG.getRoleKey(),
            SysRoleEnum.DZ_ZGSSZ.getRoleKey())) {
            throw new ServiceException("无上报权限");
        }
        DzTaskDistListAdd add = MapstructUtils.convert(bo, DzTaskDistListAdd.class);
        validEntityBeforeSave(add, true);
        add.setCreateTime(new Date());
        add.setReportUserId(LoginHelper.getUserId());
        return baseMapper.insertEntity(add) > 0;
    }

    @Override
    public Boolean updateByBo(DzTaskDistListAddBo bo) {
        if (bo.getTaskId() == null || bo.getUserId() == null) {
            throw new ServiceException("任务id与用户id不能为空");
        }
        if (queryByKeys(bo.getTaskId(), bo.getUserId()) == null) {
            throw new ServiceException("记录不存在，无法修改");
        }
        DzTaskDistListAdd entity = MapstructUtils.convert(bo, DzTaskDistListAdd.class);
        validEntityBeforeSave(entity, false);
        return baseMapper.updateByKeys(entity) > 0;
    }

    @Override
    public Boolean deleteWithValidByKeys(Long taskId, Long userId, Boolean isValid) {
        if (taskId == null || userId == null) {
            throw new ServiceException("任务id与用户id不能为空");
        }
        if (Boolean.TRUE.equals(isValid)) {
            DzTaskDistListAddVo existing = queryByKeys(taskId, userId);
            if (existing == null) {
                throw new ServiceException("记录不存在，无法删除");
            }
        }
        return baseMapper.deleteByKeys(taskId, userId) > 0;
    }

    private void validEntityBeforeSave(DzTaskDistListAdd entity, boolean insert) {
        if (entity == null) {
            throw new ServiceException("数据不能为空");
        }
        if (entity.getTaskId() == null || entity.getUserId() == null) {
            throw new ServiceException("任务id与用户id不能为空");
        }
        if (entity.getRemark() == null || entity.getRemark().isBlank()) {
            throw new ServiceException("上报内容不能为空");
        }
        if (insert) {
            DzTaskDistListAddVo dup = queryByKeys(entity.getTaskId(), entity.getUserId());
            if (dup != null) {
                throw new ServiceException("该任务与用户的上报记录已存在");
            }
        }
    }
}
