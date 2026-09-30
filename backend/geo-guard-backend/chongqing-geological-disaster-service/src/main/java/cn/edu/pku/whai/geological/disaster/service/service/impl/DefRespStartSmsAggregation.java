/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.data.domain.po.AdRegion;

import java.util.*;

/**
 * 防御响应启动短信聚合器（从 DzTaskDistListServiceImpl 提取）。
 * <p>字段保持 package-private 以兼容原调用方的直接字段访问。</p>
 */
public class DefRespStartSmsAggregation {
    final String bizKey;
    final String bizName;
    final String smsTemplate;
    final Long userId;
    final String userName;
    final String phone;
    Integer highestLevel;
    final Map<String, AdRegion> matchedRegions = new LinkedHashMap<>();
    final Map<String, Integer> visibleTownLevels = new LinkedHashMap<>();

    public DefRespStartSmsAggregation(String bizKey,
                                       String bizName,
                                       String smsTemplate,
                                       Long userId,
                                       String userName,
                                       String phone) {
        this.bizKey = bizKey;
        this.bizName = bizName;
        this.smsTemplate = smsTemplate;
        this.userId = userId;
        this.userName = userName;
        this.phone = phone;
    }

    public void accept(AdRegion region) {
        if (region == null || region.getLevel() == null || StringUtils.isBlank(region.getId())) {
            return;
        }
        if (highestLevel == null || region.getLevel() < highestLevel) {
            highestLevel = region.getLevel();
            matchedRegions.clear();
            matchedRegions.put(region.getId(), region);
            return;
        }
        if (Objects.equals(highestLevel, region.getLevel())) {
            matchedRegions.putIfAbsent(region.getId(), region);
        }
    }

    public void acceptTownLevel(String street, Integer level) {
        if (StringUtils.isBlank(street) || level == null) {
            return;
        }
        visibleTownLevels.putIfAbsent(street.trim(), level);
    }
}
