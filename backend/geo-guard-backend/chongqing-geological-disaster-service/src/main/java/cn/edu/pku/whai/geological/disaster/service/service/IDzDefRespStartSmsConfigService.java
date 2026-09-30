/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzDefRespStartSmsConfigBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzDefRespStartSmsConfigVo;

import java.util.List;

/**
 * 启动短信配置 Service 接口
 */
public interface IDzDefRespStartSmsConfigService {

    /**
     * 查询全部配置
     */
    List<DzDefRespStartSmsConfigVo> queryList();

    /**
     * 查询启用配置
     */
    List<DzDefRespStartSmsConfigVo> queryEnabledList();

    /**
     * 更新配置
     */
    Boolean updateByBo(DzDefRespStartSmsConfigBo bo);

    /**
     * 构建当前启用配置摘要
     */
    String buildEnabledConfigDigest();
}
