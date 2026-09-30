/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DzDefProcessProgressVo;
import cn.edu.pku.whai.geological.disaster.service.service.IDzProcessProgressService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SaIgnore
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/processProgress")
public class DzProcessProgressController extends BaseController {

    private final IDzProcessProgressService dzProcessProgressService;

    /**
     * 获取防御响应流程进度
     */
    @GetMapping("/def")
    public R<List<DzDefProcessProgressVo>> getDefInfo(@NotNull(message = "defId 不能为空") String defId) {
        long defIdLong;
        try {
            defIdLong = Long.parseLong(defId.trim());
        } catch (NumberFormatException e) {
            throw new ServiceException("defId 格式错误，必须为数字");
        }
        return R.ok(dzProcessProgressService.getDefInfo(defIdLong));
    }
}
