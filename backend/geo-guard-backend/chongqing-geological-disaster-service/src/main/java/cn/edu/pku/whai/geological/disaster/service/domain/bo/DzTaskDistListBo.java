package cn.edu.pku.whai.geological.disaster.service.domain.bo;

import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskDistList;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

/**
 * 任务派发清单业务对象 dz_task_dist_list
 *
 * @author kongweiguang
 * @date 2026-01-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzTaskDistList.class, reverseConvertGenerate = false)
public class DzTaskDistListBo extends BaseEntity {

    /**
     * id（支持字符串反序列化，兼容防御响应API）
     */
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    @JsonDeserialize(using = LongFromStringDeserializer.class)
    private Long id;

    /**
     * 斜坡单元id
     */
    private String unitId;

    /**
     * 派发人员id
     */
    private Long userId;

    /**
     * 风险评估id
     */
    private Long riskId;

    /**
     * 动态风险级别(0：无风险，1：低风险 2：中风险 3：高风险 4：极高风险)
     */
    private Integer dynamicRiskLevel;

    /**
     * 动态风险级别列表
     */
    private List<Integer> dynamicRiskLevels;

    /**
     * 试点区域1（data_slope_unit.pilot_area_1）
     */
    private Integer pilotArea1;

    /**
     * 试点区域2（data_slope_unit.pilot_area_2）
     */
    private Integer pilotArea2;

    /**
     * 巡查要求
     */
    private String submitRequire;

    /**
     * 巡查建议
     */
    private String inspectionSuggestion;

    /**
     * 巡查建议备份
     */
    private String inspectionSuggestionBackup;

    /**
     * 现场照片（多条的话，分割）
     */
    private String scenePhoto;

    /**
     * 文字记录
     */
    private String textRecord;

    /**
     * 任务状态（1：未推送 2：未核查 3：核查中 4：已关闭 5：已反馈 6：申请技术协查 7：已过期）
     */
    private Integer status;
    private List<Integer> statusList;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createDate;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateDate;

    /**
     * 检查时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date checkTime;

    /**
     * 检查坐标中心点
     */
    private String checkCenter;

    /**
     * 打卡坐标生成的地点信息
     */
    private String checkCenterLocation;

    /**
     * 提交时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;

    /**
     * 来源（0：手动添加 1：系统评估 2：群众上报 3：防御响应 4：应急处置 5：监测预警 6：技术协查）
     */
    private Integer sourceType;

    /**
     * 来源列表
     */
    private List<Integer> sourceTypeList;

    /**
     * 是否显示应急处置的巡查任务
     */
    private Integer isEmergency;

    /**
     * 群众上报时报告id
     */
    private Long reportId;

    /**
     * 方案编号(关联方案表唯一标识)
     */
    private Long handleId;

    /**
     * 防御响应方案id（兼容 regId）
     */
    @JsonAlias("regId")
    private Long defId;

    /**
     * 上报信息JSON（仅险情核实任务使用）
     */
    private String reportInfo;

    /**
     * 任务来源业务描述
     */
    private String taskSource;

    /**
     * 上游任务节点ID（技术协查任务填写原任务id）
     */
    private Long relatedTaskId;

    /**
     * 任务类型（巡查任务 / AI险情核实 / 防御响应 / 现场处置 / 监测预警 / 应急调查 / 群众报灾）
     */
    private String taskType;

    /**
     * 任务类型列表
     */
    private List<String> taskTypeList;

    /**
     * 任务名称
     */
//    @NotBlank(message = "任务名称不能为空", groups = {AddGroup.class})
    private String planName;

    /**
     * 任务类型: 1.人员安置/2.警示防护/3.监测巡查（群测群防）/4.排危除险/5.工程治理/6.宣传告知/7.交通管制/8.其他建议/9.监测巡查（仪器监测）
     */
//    @NotNull(message = "任务类型不能为空", groups = {AddGroup.class})
    private Integer planType;

    /**
     * 责任人
     */
//    @NotBlank(message = "责任人不能为空", groups = {AddGroup.class})
    private String responsiblePerson;

    /**
     * 责任人电话号码
     */
    private String responsiblePersonPhone;

    /**
     * 详细地址
     */
    private String detailedAddress;

    /**
     * 删除状态: 0. 未删除 1.已删除
     */
    private Integer delete;

    /**
     * 是否逾期: 0. 未逾期 1.已逾期
     */
    private Integer overdue;

    /**
     * 配额已消耗: 0.未消耗 1.已消耗（仅风险区巡查员任务有效，用户有效完成时置1，与status解耦避免混用）
     */
    private Integer quotaConsumed;

    /**
     * 最后一次催办时间
     */
    private Date lastRemindTime;

    /**
     * 催办次数
     */
    private Integer reminderCount;

    /**
     * 任务关闭原因：如区域防御随乡镇/县级归档、范围缩小等由业务写入可读文案
     */
    private String closeReason;

    /**
     * 任务关闭时间：与 closeReason 同时写入，用于审计与列表展示
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date closedTime;

    /**
     * 主表备注
     */
    private String remark;

    /**
     * 升序排序字段（按字段名）
     */
    private List<String> asc;

    /**
     * 降序排序字段（按字段名）
     */
    private List<String> desc;

    static class LongFromStringDeserializer extends JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser p, DeserializationContext ctxt) throws java.io.IOException {
            JsonToken t = p.getCurrentToken();
            if (t == JsonToken.VALUE_NUMBER_INT) {
                return p.getLongValue();
            }
            if (t == JsonToken.VALUE_STRING) {
                String s = p.getText();
                if (s == null || s.isBlank()) {
                    return null;
                }
                try {
                    return Long.parseLong(s.trim());
                } catch (NumberFormatException e) {
                    throw ctxt.weirdStringException(s, Long.class, "not a valid Long");
                }
            }
            return null;
        }
    }
}
