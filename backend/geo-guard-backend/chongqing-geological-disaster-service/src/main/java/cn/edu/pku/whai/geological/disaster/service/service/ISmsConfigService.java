/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import cn.edu.pku.whai.geological.disaster.service.domain.bo.SmsConfigUpdateBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.SmsConfigVo;
import cn.edu.pku.whai.geological.disaster.service.sms.config.SmsConfigSnapshot;

/**
 * 短信动态配置服务。
 */
public interface ISmsConfigService {

    /**
     * 获取当前有效配置快照。
     *
     * @return 当前有效配置快照
     */
    SmsConfigSnapshot getSnapshot();

    /**
     * 查询脱敏后的当前短信配置。
     *
     * @return 短信配置展示对象
     */
    SmsConfigVo getConfig();

    /**
     * 部分更新短信配置。
     *
     * @param bo 待更新字段
     * @return 更新后的脱敏配置
     */
    SmsConfigVo updateConfig(SmsConfigUpdateBo bo);

    /**
     * 删除共享缓存并从数据库重建当前快照。
     *
     * @return 刷新后的有效配置快照
     */
    SmsConfigSnapshot refreshCache();
}
