/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.app.domain.req;

import lombok.Data;

import java.util.List;

/**
 * 巡查结果提交 DTO
 * 对应APP端巡查结果提交接口，包含任务信息、巡查员信息、打卡信息、现场记录等内容
 */
@Data
public class InspectionSubmitReq {
    /**
     * 任务唯一ID，与派发时一致
     */
    private Long taskId;

    /**
     * 巡查员账号ID，派发任务时传入的
     */
    private Long inspectorId;

    /**
     * 巡查员姓名
     */
    private String inspectorName;

    /**
     * 巡查员电话
     */
    private String inspectorPhone;

    /**
     * 打卡时间（如："2026-01-08 13:25:00"）
     */
    private String checkInTime;

    /**
     * 打卡经纬度（如：POINT(109.5179247815959830.631124795779762)）
     */
    private String checkInCenter;

    /**
     * 现场照片的OSS ID列表，由APP上传至OSS后返回的ossId
     */
    private List<String> photos;

    /**
     * 文字记录，格式建议："发现裂缝" 或 "无明显异常" 若含自定义内容，可拼接如："其他险情：坡顶有积水"
     */
    private String textRecord;

    /**
     * 提交时间（如："2026-01-08 13:25:00"）
     */
    private String submitTime;

    /**
     * 任务状态（1：未推送 2：未核查 3：核查中 4：已关闭 5：已反馈 6：申请技术协查 7：已过期）
     */
    private Integer status;

    /**
     * 用户角色，用于区分不同权限用户提交的数据
     */
    private String role;

    /**
     * 检查点的位置（中文），描述检查点的具体位置信息
     */
    private String checkCenterLocation;
}
