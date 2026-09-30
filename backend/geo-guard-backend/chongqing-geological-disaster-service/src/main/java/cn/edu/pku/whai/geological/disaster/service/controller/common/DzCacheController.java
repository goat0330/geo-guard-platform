/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller.common;

import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.cache.DzBusinessCacheRedisCache;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzBusinessCacheBo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通用业务缓存控制器
 *
 * @author whai
 */
@Validated
@RestController
@RequestMapping("/dizai/common/dzCache")
public class DzCacheController {

    private static final Integer GLOBAL_SCOPE = 1;
    private static final Long GLOBAL_SCOPE_USER_ID = 0L;

    /**
     * 记录当前登录用户当天已处理过的业务标记，并把全局范围归一到固定用户 ID。
     */
    @Log(title = "通用业务缓存", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @PostMapping("/record")
    public R<Void> record(@Validated @RequestBody DzBusinessCacheBo bo) {
        DzBusinessCacheRedisCache.record(bo.getBusinessType(), resolveUserId(bo), bo.getBusinessContent());
        return R.ok();
    }

    /**
     * 查询当前登录用户当天是否已存在业务标记，保持与记录接口相同的范围解析规则。
     */
    @PostMapping("/query")
    public R<Boolean> query(@Validated @RequestBody DzBusinessCacheBo bo) {
        return R.ok(DzBusinessCacheRedisCache.hasRecord(bo.getBusinessType(), resolveUserId(bo)));
    }

    /**
     * 将全局缓存映射到固定主体，其余缓存按当前登录用户隔离。
     */
    private Long resolveUserId(DzBusinessCacheBo bo) {
        if (GLOBAL_SCOPE.equals(bo.getScope())) {
            return GLOBAL_SCOPE_USER_ID;
        }
        return LoginHelper.getUserId();
    }
}
