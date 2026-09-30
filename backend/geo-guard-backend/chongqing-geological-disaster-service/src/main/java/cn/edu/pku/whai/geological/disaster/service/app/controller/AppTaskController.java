package cn.edu.pku.whai.geological.disaster.service.app.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.InspectionSubmitReq;
import cn.edu.pku.whai.geological.disaster.service.app.service.IAppTaskService;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskDistListHistoryVo;
import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * APP任务
 *
 * @author kongweiguang
 */
@RestController
@RequestMapping("/v1/tasks")
@RequiredArgsConstructor
public class AppTaskController {
    private final IAppTaskService appTaskService;

    /**
     * 巡查结果提交接口
     */
    @Log(title = "APP巡查结果", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PostMapping("/submit")
    public R<Void> submitTask(@RequestBody InspectionSubmitReq req) {
        appTaskService.submitTask(req);
        return R.ok();
    }


    /**
     * OSS图片上传接口
     */
    @Log(title = "APP任务图片", businessType = BusinessType.INSERT, isSaveRequestData = false, operatorType = OperatorType.PLATFORM)
    @PostMapping("/upload")
    public R<List<String>> uploadImages(@RequestParam(value = "taskId", required = false) Long taskId,
                                        @RequestParam("files") MultipartFile[] files) {
        if (ObjUtil.isNull(files)) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        List<String> fr = appTaskService.uploadImages(taskId, files);
        return R.ok(fr);
    }

    /**
     * 照片预览接口，根据ossId列表获取可访问的预览URL
     * ossIds支持逗号分隔的字符串，如: 1,2,3
     */
    @GetMapping("/photos/preview")
    public R<List<String>> photoPreview(@RequestParam("ossIds") String ossIds) {
        if (ObjUtil.isNull(ossIds) || ossIds.isBlank()) {
            throw new IllegalArgumentException("ossIds不能为空");
        }
        List<String> ids = Arrays.stream(ossIds.split(","))
                                 .map(String::trim)
                                 .filter(s -> !s.isEmpty())
                                 .toList();
        List<String> urls = appTaskService.getPhotoPreviewUrls(ids);
        return R.ok(urls);
    }

    /**
     * 查询已反馈内容
     */
    @GetMapping("/feedbacks")
    public R<List<DzTaskDistListHistoryVo>> feedbacks(@RequestParam("taskId") Long taskId) {
        return R.ok(appTaskService.feedbacks(taskId));
    }
}
