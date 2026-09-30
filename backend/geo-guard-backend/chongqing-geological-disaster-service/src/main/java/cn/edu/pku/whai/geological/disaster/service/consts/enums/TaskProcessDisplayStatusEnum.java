/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.consts.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程链路列表当前状态。
 */
@Getter
@AllArgsConstructor
public enum TaskProcessDisplayStatusEnum {

    FLOWING(1, "流转中"),
    EXECUTING(2, "执行中"),
    FEEDBACKED(3, "已反馈"),
    TECH_ASSIST(4, "技术协查"),
    OVERDUE(5, "已过期"),
    FINISHED(6, "已结束");

    private final Integer code;
    private final String label;

    public static TaskProcessDisplayStatusEnum of(Integer code) {
        if (code == null) {
            return FLOWING;
        }
        for (TaskProcessDisplayStatusEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return FLOWING;
    }

    public static TaskProcessDisplayStatusEnum fromLinkName(String linkName) {
        if (StrUtil.isBlank(linkName)) {
            return FLOWING;
        }
        String displayName = TaskProcessChainNodeTextEnum.normalizeDisplayLinkName(linkName);
        if (isAny(displayName, "AI险情核实任务反馈")) {
            return FINISHED;
        }
        if (isAny(displayName, "技术协查任务推送")) {
            return TECH_ASSIST;
        }
        String normalized = TaskProcessChainNodeTextEnum.normalizeSemanticLinkName(linkName);
        if (isAny(normalized, "任务关闭", "结束归档", "防御响应结束归档", "灾险情关闭")) {
            return FINISHED;
        }
        if ("任务过期".equals(normalized)) {
            return OVERDUE;
        }
        if (isAny(normalized, "任务核查中", TaskProcessChainNodeTextEnum.LEGACY_TASK_INSPECTING_LINK_NAME)) {
            return EXECUTING;
        }
        if (isAny(normalized, "任务反馈", "任务反馈报告", "上报现场处置报告", "任务反馈并上报现场处置报告",
            "技术协查任务反馈并上报现场处置报告")) {
            return FEEDBACKED;
        }
        if (isAny(normalized, "申请技术协查", "下发技术协查任务")) {
            return TECH_ASSIST;
        }
        return FLOWING;
    }

    public static String label(Integer code) {
        return of(code).label;
    }

    private static boolean isAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (candidate.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
