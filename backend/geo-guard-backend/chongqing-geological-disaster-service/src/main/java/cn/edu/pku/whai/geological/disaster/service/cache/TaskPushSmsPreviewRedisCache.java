/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.cache;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistSmsPreviewItemVo;
import cn.hutool.json.JSONUtil;

import java.time.Duration;
import java.util.List;

/**
 * 任务推送短信预览缓存
 */
public class TaskPushSmsPreviewRedisCache {

    private static final String TASK_PUSH_SMS_PREVIEW_KEY = "task_push_sms_preview:";
    private static final Duration EXPIRE_DURATION = Duration.ofMinutes(5);

    private TaskPushSmsPreviewRedisCache() {
    }

    public static void setPreview(String previewKey, List<TaskDistSmsPreviewItemVo> previewList) {
        if (StringUtils.isBlank(previewKey) || previewList == null || previewList.isEmpty()) {
            return;
        }
        RedisUtils.setCacheObject(buildKey(previewKey), JSONUtil.toJsonStr(previewList), EXPIRE_DURATION);
    }

    public static List<TaskDistSmsPreviewItemVo> getPreview(String previewKey) {
        if (StringUtils.isBlank(previewKey)) {
            return List.of();
        }
        String raw = RedisUtils.getCacheObject(buildKey(previewKey));
        if (StringUtils.isBlank(raw)) {
            return List.of();
        }
        return JSONUtil.toList(JSONUtil.parseArray(raw), TaskDistSmsPreviewItemVo.class);
    }

    private static String buildKey(String previewKey) {
        return TASK_PUSH_SMS_PREVIEW_KEY + previewKey.trim();
    }
}
