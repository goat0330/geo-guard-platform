/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.utils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Run {
    public static final Executor executor = Executors.newVirtualThreadPerTaskExecutor();
}
