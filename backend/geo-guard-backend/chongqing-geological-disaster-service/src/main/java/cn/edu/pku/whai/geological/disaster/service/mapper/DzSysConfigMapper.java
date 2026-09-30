/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.mapper;

import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSysConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 地灾侧系统参数配置 Mapper，避免与系统模块 SysConfigMapper bean 名冲突。
 */
public interface DzSysConfigMapper extends BaseMapper<DzSysConfig> {
}
