package cn.edu.pku.whai.geological.disaster.service.app.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.DzTaskHandleReq;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.DzTaskHandleSceneRecordReq;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * APP任务处置相关接口
 *
 * @author kongweiguang
 */
@Validated
@RestController
@RequestMapping("/v1/tasks/handle")
@RequiredArgsConstructor
public class AppTaskHandleController {
    private final IDzTaskHandleService dzTaskHandleService;


    /**
     * 新增处置管理
     */
    @Log(title = "APP处置管理", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PostMapping("addHandle")
    public R<Long> addHandle(@RequestBody DzTaskHandleReq req) {
        Long handleId = dzTaskHandleService.insertByReq(req);
        return R.ok(handleId);
    }

    /**
     * 新增处置管理现场记录
     */
    @Log(title = "APP处置现场记录", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @RepeatSubmit()
    @PostMapping("addSceneRecord")
    public R<Void> addSceneRecord(@Validated(AddGroup.class) @RequestBody DzTaskHandleSceneRecordReq req) {
        dzTaskHandleService.addSceneRecord(req);
        return R.ok();
    }


}
