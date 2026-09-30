package cn.edu.pku.whai.controller;

import org.dromara.common.core.config.BwyConfig;
import org.dromara.common.core.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页
 *
 * @author kongweiguang
 */
@RequiredArgsConstructor
@RestController
public class IndexController {

    /**
     * 系统基础配置
     */
    private final BwyConfig bwyConfig;

    /**
     * 从公共配置读取服务身份，避免迁移后首页仍展示写死的原项目名称。
     */
    @GetMapping("/")
    public String index() {
        return StringUtils.format("当前服务：{}，当前版本：{}。", bwyConfig.getName(), bwyConfig.getVersion());
    }
}
