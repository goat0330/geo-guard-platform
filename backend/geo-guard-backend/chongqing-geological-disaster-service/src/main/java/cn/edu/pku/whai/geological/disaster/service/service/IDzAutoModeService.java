/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import cn.edu.pku.whai.geological.disaster.service.domain.bo.AutoModeConfigBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AutoModeStatusVo;

/**
 * 地灾自动模式 Service。
 */
public interface IDzAutoModeService {

    /**
     * 查询自动模式当前状态。
     *
     * @return 自动模式状态
     */
    AutoModeStatusVo getStatus();

    /**
     * 开启自动模式。
     *
     * @param bo 自动模式配置；为空时仅开启开关
     * @return 开启后的自动模式状态
     */
    AutoModeStatusVo open(AutoModeConfigBo bo);

    /**
     * 关闭自动模式。
     *
     * @return 关闭后的自动模式状态
     */
    AutoModeStatusVo close();

    /**
     * 更新自动模式配置。
     *
     * @param bo 自动模式配置
     * @return 更新后的自动模式状态
     */
    AutoModeStatusVo updateConfig(AutoModeConfigBo bo);

    /**
     * 执行一次自动模式调度。
     */
    void runOnce();
}
