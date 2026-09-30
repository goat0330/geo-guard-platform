/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts;

/**
 * 区域防御响应关闭原因常量（与最终方案文档对齐）。
 */
public final class RegionDefRespCloseReason {

    private RegionDefRespCloseReason() {
    }

    /** 范围缩小，乡镇响应关闭 */
    public static final String TOWN_RANGE_SHRINK = "范围缩小，乡镇响应关闭";

    /** 手动关闭乡镇级防御响应 */
    public static final String TOWN_MANUAL_CLOSE = "手动关闭乡镇级防御响应";

    /** 县级响应结束归档 */
    public static final String TOWN_ARCHIVE_WITH_COUNTY = "县级响应结束归档";
}
