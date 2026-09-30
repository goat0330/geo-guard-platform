/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.service.IAdRegionService;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.SysRoleEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzUserAdRegionVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzUserAdRegionService;
import cn.edu.pku.whai.geological.disaster.service.utils.CurrentRoleUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
class DzTaskHandlePermissionSupport {

    private final DzTaskHandleMapper taskHandleMapper;
    private final IDzUserAdRegionService dzUserAdRegionService;
    private final IAdRegionService adRegionService;

    DzTaskHandle requireTaskHandleById(Long id) {
        if (id == null) {
            throw new ServiceException("任务不存在");
        }
        DzTaskHandle taskHandle = taskHandleMapper.selectById(id);
        if (taskHandle == null) {
            throw new ServiceException("任务不存在");
        }
        return taskHandle;
    }

    void validateCurrentDutyOfficerTaskHandleAccess(DzTaskHandle taskHandle) {
        if (!CurrentRoleUtil.hasRole(SysRoleEnum.DZ_ZBY.getRoleKey())) {
            throw new ServiceException("仅值班员允许执行该操作");
        }
    }

    void validateCurrentUserTaskHandleAccess(
        String province,
        String city,
        String county,
        String street,
        String village
    ) {
        // 处置管理不再按用户绑定行政区划做访问校验。
    }

    void appendCurrentUserTaskHandlePermission(LambdaQueryWrapper<DzTaskHandle> lqw) {
        // 处置管理列表不再追加当前用户行政区划过滤条件。
    }

    List<AdRegionVo> listCurrentUserRegions(String actionMessage) {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("当前用户未登录或登录已失效");
        }
        List<String> adRegionIds = dzUserAdRegionService.queryEffectiveListByUserId(userId)
                                                        .stream()
                                                        .filter(item -> item != null && StringUtils.isNotBlank(item.getAdRegionId()))
                                                        .map(DzUserAdRegionVo::getAdRegionId)
                                                        .distinct()
                                                        .toList();
        if (adRegionIds.isEmpty()) {
            return List.of();
        }
        List<AdRegionVo> userRegions = adRegionIds.stream()
                                                  .map(adRegionService::getInfo)
                                                  .filter(Objects::nonNull)
                                                  .toList();
        if (userRegions.isEmpty()) {
            throw new ServiceException("当前用户关联的行政区划不存在，" + actionMessage);
        }
        return userRegions;
    }
}
