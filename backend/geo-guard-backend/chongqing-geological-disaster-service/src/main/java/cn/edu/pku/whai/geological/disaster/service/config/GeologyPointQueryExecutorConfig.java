/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 点位地质信息查询线程池配置
 *
 * @author zhuzc
 * @date 2026-06-24
 */
@Configuration
public class GeologyPointQueryExecutorConfig {

    private static final int POINT_QUERY_PARALLELISM = 8;

    private static final int POINT_QUERY_QUEUE_CAPACITY = 64;

    @Bean(name = "geologyPointQueryExecutor", destroyMethod = "shutdown")
    public ExecutorService geologyPointQueryExecutor() {
        return new ThreadPoolExecutor(
            POINT_QUERY_PARALLELISM,
            POINT_QUERY_PARALLELISM,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(POINT_QUERY_QUEUE_CAPACITY),
            newNamedThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private static ThreadFactory newNamedThreadFactory() {
        AtomicInteger threadIndex = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("geology-point-query-" + threadIndex.getAndIncrement());
            return thread;
        };
    }
}
