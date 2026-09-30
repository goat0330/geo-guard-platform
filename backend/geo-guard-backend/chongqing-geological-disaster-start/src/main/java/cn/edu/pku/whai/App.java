// @author kongweiguang
package cn.edu.pku.whai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * 启动程序
 *
 * @author kongwegiuang
 */
@SpringBootApplication(scanBasePackages = {"cn.edu.pku.whai", "org.dromara"})
public class App {

    /**
     * 沿用公共根包扫描和启动指标缓冲，使 BWY 公共组件与迁入的 data 组件保持原装配边界。
     */
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(App.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ  App启动成功   ლ(´ڡ`ლ)ﾞ");
    }

}
