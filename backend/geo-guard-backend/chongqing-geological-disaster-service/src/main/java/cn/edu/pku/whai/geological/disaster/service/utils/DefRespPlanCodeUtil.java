/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

/**
 * 防御响应方案编号生成工具。
 */
public final class DefRespPlanCodeUtil {

    public static final String MARKER_COUNTY = "xianji";
    public static final String MARKER_TOWN = "xiangzhen";
    public static final String MARKER_SINGLE = "dandian";

    private DefRespPlanCodeUtil() {
    }

    public static String buildCode(String marker) {
        return resolvePrefix(marker)
            + new SimpleDateFormat("yyyyMMdd").format(new Date())
            + String.format("%08d", new Random().nextInt(100_000_000));
    }

    private static String resolvePrefix(String marker) {
        if (Objects.equals(MARKER_COUNTY, marker)) {
            return "XJ";
        }
        if (Objects.equals(MARKER_TOWN, marker)) {
            return "XZ";
        }
        if (Objects.equals(MARKER_SINGLE, marker)) {
            return "DD";
        }
        return "FY";
    }
}
