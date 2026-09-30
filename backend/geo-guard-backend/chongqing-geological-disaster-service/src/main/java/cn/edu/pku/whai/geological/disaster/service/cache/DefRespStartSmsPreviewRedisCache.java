/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.cache;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespStartSmsPreviewVo;
import cn.edu.pku.whai.geological.disaster.service.sms.defresp.DefRespSmsEventSnapshot;
import cn.hutool.json.JSONUtil;

import java.time.Duration;
import java.util.List;

/**
 * 启动短信预览缓存
 */
public class DefRespStartSmsPreviewRedisCache {

    private static final String DEF_RESP_START_SMS_PREVIEW_KEY = "def_resp_start_sms_preview:";
    private static final Duration EXPIRE_DURATION = Duration.ofMinutes(5);

    private DefRespStartSmsPreviewRedisCache() {
    }

    public static void setPreview(Long defId, String configDigest, List<DefRespStartSmsPreviewVo> previewList) {
        if (defId == null || StringUtils.isBlank(configDigest)) {
            return;
        }
        RedisUtils.setCacheObject(buildKey(defId, configDigest), JSONUtil.toJsonStr(previewList), EXPIRE_DURATION);
    }

    public static List<DefRespStartSmsPreviewVo> getPreview(Long defId, String configDigest) {
        if (defId == null || StringUtils.isBlank(configDigest)) {
            return List.of();
        }
        String raw = RedisUtils.getCacheObject(buildKey(defId, configDigest));
        if (StringUtils.isBlank(raw)) {
            return List.of();
        }
        return JSONUtil.toList(JSONUtil.parseArray(raw), DefRespStartSmsPreviewVo.class);
    }

    /**
     * 缓存区域防御响应生命周期短信预览。
     */
    public static void setLifecyclePreview(DefRespSmsEventSnapshot event, String configDigest,
                                           List<DefRespStartSmsPreviewVo> previewList) {
        if (event == null || StringUtils.isBlank(configDigest)) {
            return;
        }
        RedisUtils.setCacheObject(buildLifecyclePreviewKey(event, configDigest), JSONUtil.toJsonStr(previewList), EXPIRE_DURATION);
    }

    /**
     * 获取区域防御响应生命周期短信预览。
     */
    public static List<DefRespStartSmsPreviewVo> getLifecyclePreview(DefRespSmsEventSnapshot event, String configDigest) {
        if (event == null || StringUtils.isBlank(configDigest)) {
            return List.of();
        }
        String raw = RedisUtils.getCacheObject(buildLifecyclePreviewKey(event, configDigest));
        if (StringUtils.isBlank(raw)) {
            return List.of();
        }
        return JSONUtil.toList(JSONUtil.parseArray(raw), DefRespStartSmsPreviewVo.class);
    }

    /**
     * 生命周期缓存键包含配置、动作、来源、轮次和事件快照摘要，避免不同事件错误复用正文。
     */
    public static String buildLifecyclePreviewKey(DefRespSmsEventSnapshot event, String configDigest) {
        if (event == null || StringUtils.isBlank(configDigest)) {
            return null;
        }
        return DEF_RESP_START_SMS_PREVIEW_KEY + event.defId() + ":" + configDigest.trim()
            + ":" + event.action().name().toLowerCase()
            + ":" + event.source().name().toLowerCase()
            + ":" + event.roundNo()
            + ":" + event.snapshotHash();
    }

    private static String buildKey(Long defId, String configDigest) {
        return DEF_RESP_START_SMS_PREVIEW_KEY + defId + ":" + configDigest.trim();
    }
}
