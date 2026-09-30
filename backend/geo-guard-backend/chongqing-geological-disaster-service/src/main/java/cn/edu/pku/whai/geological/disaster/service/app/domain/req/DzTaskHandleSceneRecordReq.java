package cn.edu.pku.whai.geological.disaster.service.app.domain.req;

import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import cn.edu.pku.whai.geological.disaster.data.utils.JacksonUtil;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandleSceneRecord;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * app处置管理现场记录业务请求对象
 *
 * @author kongweiguang
 * @date 2026-01-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DzTaskHandleSceneRecord.class, reverseConvertGenerate = false)
public class DzTaskHandleSceneRecordReq extends BaseEntity {

    /**
     * 上报人
     */
    @NotBlank(message = "上报人不能为空", groups = AddGroup.class)
    private String reporter;

    /**
     * handle_id
     */
    @NotNull(message = "handleId不能为空", groups = AddGroup.class)
    private Long handleId;

    /**
     * APP应急调查任务ID
     */
    private String taskId;

    /**
     * 灾害地点：需精确到组或具体点位
     */
    @NotBlank(message = "灾害地点不能为空", groups = AddGroup.class)
    private String location;

    /**
     * 灾害类型：滑坡/崩塌/地面塌陷/泥石流/危岩/其他
     */
    @NotBlank(message = "灾害类型不能为空", groups = AddGroup.class)
    private String disasterType;

    /**
     * 灾害名称：自动生成逻辑：地点 + 灾害类型
     */
    @NotBlank(message = "灾害名称不能为空", groups = AddGroup.class)
    private String disasterName;

    /**
     * 打卡坐标：建议通过定位获取 例如POINT Z (109.90846238173602 30.298251166006768 1001)
     */
    @NotBlank(message = "打卡坐标不能为空", groups = AddGroup.class)
    private String checkInCoordinates;

    /**
     * 发生时间：精确到分
     */
    @NotNull(message = "发生时间不能为空", groups = AddGroup.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date occurrenceTime;

    /**
     * 发生原因：下拉选项(降雨/人类工程活动/溶蚀/风化/倾斜作用)
     */
    @NotBlank(message = "发生原因不能为空", groups = AddGroup.class)
    private String causeType;

    /**
     * 原因补充说明：对应输入框“补充说明”
     */
    private String causeRemark;

    /**
     * 威胁人员-户数 (单位：户)
     */
    @NotNull(message = "威胁户数不能为空", groups = AddGroup.class)
    @Min(value = 0, message = "威胁户数不能为负数", groups = AddGroup.class)
    private Integer threatHouseholds;

    /**
     * 威胁人员-人数 (单位：人)
     */
    @NotNull(message = "威胁人数不能为空", groups = AddGroup.class)
    @Min(value = 0, message = "威胁人数不能为负数", groups = AddGroup.class)
    private Integer threatPeople;

    /**
     * 威胁房屋 (单位：栋)
     */
    @NotNull(message = "威胁房屋不能为空", groups = AddGroup.class)
    @Min(value = 0, message = "威胁房屋不能为负数", groups = AddGroup.class)
    private Integer threatHouses;

    /**
     * 其他威胁对象：文本描述，默认无
     */
    private String otherThreats;

    /**
     * 直接经济损失 (单位：万元)
     */
    @NotNull(message = "直接经济损失不能为空", groups = AddGroup.class)
    private BigDecimal directLoss;

    /**
     * 受伤人数 (单位：人)
     */
    @NotNull(message = "受伤人数不能为空", groups = AddGroup.class)
    @Min(value = 0, message = "受伤人数不能为负数", groups = AddGroup.class)
    private Integer casualties;

    /**
     * 参与调查单位：多选，填写参与部门全称
     */
    @NotBlank(message = "参与调查单位不能为空", groups = AddGroup.class)
    private String investigationUnit;

    /**
     * 调查人：填写调查人员姓名
     */
    @NotBlank(message = "调查人不能为空", groups = AddGroup.class)
    private String investigator;

    /**
     * 调查时间：精确到年月日
     */
    @NotNull(message = "调查时间不能为空", groups = AddGroup.class)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date investigationDate;

    /**
     * 地层岩性：大段文本描述
     */
    @NotBlank(message = "地层岩性不能为空", groups = AddGroup.class)
    private String lithologyDesc;

    /**
     * 地质构造：大段文本描述
     */
    private String structureDesc;

    /**
     * 节理裂隙
     */
    private String jointFissureDesc;

    /**
     * 灾害规模  长 (单位：米)
     */
    @NotNull(message = "灾害规模长度不能为空", groups = AddGroup.class)
    private BigDecimal scaleLength;

    /**
     * 灾害规模  宽 (单位：米)
     */
    @NotNull(message = "灾害规模宽度不能为空", groups = AddGroup.class)
    private BigDecimal scaleWidth;

    /**
     * 灾害规模  厚/深 (单位：米)
     */
    @NotNull(message = "灾害规模厚深不能为空", groups = AddGroup.class)
    private BigDecimal scaleDepth;

    /**
     * 灾害规模  估算体积 (单位：m³)
     */
    @NotNull(message = "灾害规模估算体积不能为空", groups = AddGroup.class)
    private BigDecimal scaleVolume;

    /**
     * 规模等级：巨型/特大型/大型/中型/小型
     */
    private String scaleLevel;

    /**
     * 现状稳定性：枚举值(不稳定、欠稳定、基本稳定、稳定)
     */
    @NotBlank(message = "现状稳定性不能为空", groups = AddGroup.class)
    private String stabilityStatus;

    /**
     * 发展趋势：枚举值(即将失稳破坏、可能继续变形、趋于稳定)
     */
    @NotBlank(message = "发展趋势不能为空", groups = AddGroup.class)
    private String devTrend;

    /**
     * 人员安置：填写户数/人数（临时避险安置/监测居住/另行选址安置）
     */
    private String measuresEvacuation;

    /**
     * 人员安置补充信息：补充填写安置地点、安置方式、执行情况等
     */
    private String measuresEvacuationRemark;

    /**
     * 警示防护：拉设警戒线/设置警示标牌/道路封闭
     */
    private String measuresProtection;

    /**
     * 警示防护补充信息：补充填写警戒范围、警示设施布设情况等
     */
    private String measuresProtectionRemark;

    /**
     * 监测巡查：填写监测频次/重点监测内容（仪器监测/群测群防）
     */
    private String measuresMonitoring;

    /**
     * 监测巡查补充信息：补充填写巡查路线、监测点位、责任人员等
     */
    private String measuresMonitoringRemark;

    /**
     * 排危除险：清理松散土体/孤石危岩、修复挡墙排水沟、充填塌陷口
     */
    private String measuresHazardRemoval;

    /**
     * 排危除险补充信息：补充填写排危部位、处置内容、完成情况等
     */
    private String measuresHazardRemovalRemark;

    /**
     * 工程治理：聘请专业单位设计施工/抗滑桩/挡墙支护/边坡综合治理
     */
    private String measuresEngineering;

    /**
     * 工程治理补充信息：补充填写治理方案、实施单位、推进情况等
     */
    private String measuresEngineeringRemark;

    /**
     * 宣传告知：填写发放范围（发放防灾工作明白卡/避险工作明白卡）
     */
    private String measuresPublicity;

    /**
     * 宣传告知补充信息：补充填写告知对象、告知方式、覆盖范围等
     */
    private String measuresPublicityRemark;

    /**
     * 交通管制：填写道路信息（观察通行/限制通行/禁止通行）
     */
    private String measuresTrafficControl;

    /**
     * 交通管制补充信息：补充填写管制路段、管制时段、绕行方案等
     */
    private String measuresTrafficControlRemark;

    /**
     * 其他建议：灾后自救要求、房屋安全鉴定、道路改线等
     */
    private String measuresOtherSuggestions;

    /**
     * 其他建议补充信息：补充填写建议依据、实施条件、注意事项等
     */
    private String measuresOtherSuggestionsRemark;

    /**
     * 已采取措施
     */
    @Size(max = 500, message = "已采取措施最多500个汉字", groups = AddGroup.class)
    private String takenMeasures;

    /**
     * 灾害区全貌照片 (ossid, 采用, 分割)
     */
    @NotBlank(message = "灾害区全貌照片不能为空", groups = AddGroup.class)
    private String photoPanorama;

    /**
     * 变形特征照片 JSON 数组
     */
    @JsonDeserialize(using = JsonArrayStringDeserializer.class)
    private String photoDeformation;

    /**
     * 受损情况照片 JSON 数组
     */
    @JsonDeserialize(using = JsonArrayStringDeserializer.class)
    private String photoDamage;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createDate;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateDate;

    /**
     * 灾害点的坐标 POINT Z (109.90846238173602 30.298251166006768 1001)
     */
    @NotBlank(message = "灾害点坐标不能为空", groups = AddGroup.class)
    private String disasterCoordinates;

    /**
     * 危险范围 WKT（文本存储，对应库列 hazard_extent）
     */
    @NotBlank(message = "危险范围不能为空", groups = AddGroup.class)
    private String hazardExtent;

    /**
     * 风险范围 WKT（文本存储，对应库列 risk_extent）
     */
    @NotBlank(message = "风险范围不能为空", groups = AddGroup.class)
    private String riskExtent;

    /**
     * 灾害性质：土质/岩质/岩土混合
     */
    private String disasterNature;

    /**
     * 威胁资产（万元）
     */
    @NotNull(message = "威胁资产不能为空", groups = AddGroup.class)
    private BigDecimal threatAsset;

    /**
     * 间接经济损失（万元）
     */
    @NotNull(message = "间接经济损失不能为空", groups = AddGroup.class)
    private BigDecimal indirectLoss;

    /**
     * 死亡人数（人）
     */
    @NotNull(message = "死亡人数不能为空", groups = AddGroup.class)
    @Min(value = 0, message = "死亡人数不能为负数", groups = AddGroup.class)
    private Integer deadPeople;

    /**
     * 其他危害文本描述
     */
    private String otherDanger;

    /**
     * 斜坡结构：平缓层状/顺向/横向/逆向/斜向/特殊结构
     */
    @NotBlank(message = "斜坡结构不能为空", groups = AddGroup.class)
    private String slopeStructure;

    /**
     * 主滑方向（度）
     */
    private BigDecimal slidingDirection;

    /**
     * 滑坡形态：半圆/矩形/舌形/不规则
     */
    private String landsideMorphology;

    /**
     * 专项特征 (以滑坡为例)
     * 字段名称 (中文)	建议字段名 (Code)	数据类型 (Type)	必填	备注/逻辑说明
     * 滑坡形态   landslide_morphology  Varchar  是  下拉框：半圆/矩形/舌形/不规则
     * 滑坡边界	landslide_boundary	Varchar	是	文本描述：描述前缘、后缘、两侧边界位置
     * 滑体	sliding_mass	Varchar	是	文本描述
     * 滑面	sliding_surface	Varchar	是	下拉框：老滑面/层理面/片理或劈理面/节理裂隙面/覆盖面与基岩接触面/层内错动带/构造错动带/断层
     * 规模等级	scale_level	Varchar	是	下拉框：特大型/大型/中型/小型	特大型/大型/中型/小型	单
     * 变形活动阶段	deformation_stage	Varchar	是	下拉框：初始蠕动阶段/加速变形阶段/剧烈变形阶段/破坏阶段/休止阶段
     * 变形部位	deformation_part	Varchar	是	文本描述
     * 变形特征	deformation_char	Text	是	文本描述：拉张裂缝、下挫台阶等描述
     * 变形迹象	deformation_signs	Varchar	是	下拉框：拉涨裂缝/剪切裂缝/地面隆起/地面沉降/剥、坠落/树木歪斜/建筑变形/渗冒混水
     * 特殊说明	special_notes	Text	是	文本描述：切坡建房情况、支护措施等
     */
    private Map<String, Object> specialCharacteristics;

    static class JsonArrayStringDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.readValueAsTree();
            if (node == null || node.isNull()) {
                return null;
            }
            if (node.isTextual()) {
                return node.asText();
            }
            if (!node.isArray()) {
                throw ctxt.weirdStringException(node.toString(), String.class, "照片字段必须为JSON数组或字符串");
            }
            return JacksonUtil.toJson(node);
        }
    }

}
