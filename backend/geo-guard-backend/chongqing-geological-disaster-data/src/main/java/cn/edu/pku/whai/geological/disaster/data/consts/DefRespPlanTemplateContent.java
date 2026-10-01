/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.consts;

import cn.edu.pku.whai.geological.disaster.data.utils.LevelCodeUtil;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 防御响应方案模板内容常量，支持占位符填充。
 */
public final class DefRespPlanTemplateContent {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\w+)}");

    /**
     * 区域防御响应方案模板头部。
     */
    private static final String REG_PLAN_TEMPLATE_PREFIX = """
        ## 气象预警类区域防御响应方案

        ### 一、基本信息

        - **恩施市整体**：{default_level_text}
        - **局部区域**：{response_level_text}
        - **启动时间**：{start_time}
        - **启动条件**：{trigger_condition}

        ### 二、总则

        #### （一）编制目的

        为规范{specific_area_name}气象预警类防御响应工作，建立“县级统筹联动、乡镇快速响应、村级精准落实、网格实时巡查”四级防御体系，明确各级职责与操作流程，高效应对暴雨、台风、暴雪等气象灾害引发的次生风险，保障群众生命财产安全，减少灾害损失，结合区域气象灾害特点制定本方案。

        #### （二）适用范围

        本方案适用于{specific_area_name}内各类气象预警发布后的预警传达、风险防范、应急响应、处置善后等工作，覆盖县、乡、村、网格四级响应主体的职责落实与协同联动。

        #### （三）响应分级依据

        根据气象预警等级、预计影响时长、强度，结合{specific_area_name}地形地质条件、承灾能力及历史灾害情况，划分为四级响应，具体标准如下：

        - **红色级**：接收到地质灾害气象风险预警红色预警信息，区域发生地质灾害风险极高。
        - **橙色级**：接收到地质灾害气象风险预警橙色预警信息，区域发生地质灾害风险高。
        - **黄色级**：接收到地质灾害气象风险预警黄色预警信息，区域发生地质灾害风险较高。
        - **蓝色级（一般）**：接收到地质灾害气象风险预警蓝色预警信息，区域发生地质灾害风险一般。

        ### 三、各级响应人员构成及核心职责
        """;
    private static final String REG_PLAN_RED_SECTION = """
        #### （一）红色响应：人员职责

        1. **县级响应人员**

           - **分管县长**：担任总指挥，第一时间传达气象预警信息，统筹全县应急资源，下达紧急防御与处置指令；对接上级部门汇报预警应对进展；协调气象、水利、交通、住建等部门联动，保障抢险救援、群众转移、物资供应等工作。
           - **自然资源部门分管负责人**：实时跟踪气象风险预警更新情况，联合技术团队研判次生灾害风险，及时发布预警升级或解除信息。
           - **行业部门分管负责人（水利、交通、住建、民政等）**：依据气象预警类型（如暴雨、台风），落实本行业专项防御措施，如河道疏浚、道路管控、危房排查、安置点建设等。
           - **技术支撑单位分区负责人**：带队赶赴高风险区域，结合气象数据开展次生灾害隐患排查与监测，提供技术指导，防范二次灾害。

        2. **乡镇级响应人员**

           - **乡分管责任人**：快速传达气象预警信息，落实县级指令，统筹本乡镇防御工作；组织高风险区域群众紧急转移安置，实时上报灾害及处置信息，对接县级部门做好应急物资收发。
           - **管理员**：实时接收气象预警更新，传递给村级及网格级人员，汇总上报辖区气象影响情况与隐患信息。
           - **技术单位协管员**：对接县级技术部门，反馈辖区气象灾害影响实况，指导基层开展科学防御。

        3. **村级响应人员**

           - **专管员**：第一时间向村民传达气象预警信息，组织高风险区域群众排查转移，建立转移台账，及时上报人员动态与隐患信息，组织隐患巡查及物资分发。
           - **风险区行业部门联防员**：针对气象预警可能引发的本行业风险（如水利设施险情、道路积水），强化排查监测，及时上报并协同处置。

        4. **网格级响应人员**

           - **监测员**：按频次（{red_monitor_frequency}，如：每4小时1次）巡查风险点，异常情况第一时间上报，协助转移群众。
           - **风险区巡查员**：每日至少开展 {red_patrol_times} 次（如：4次）巡查，重点排查河道、边坡、危房等易受气象灾害影响区域，及时上报隐患并协助避险。
        """;

    private static final String REG_PLAN_ORANGE_SECTION = """
        #### （一）橙色响应：人员职责

        1. **县级响应人员**

           - **分管县长**：统筹应急处置工作，传达气象预警信息，部署核心防御任务；对接上级争取支持，协调各部门高效调配应急资源。
           - **自然资源部门分管负责人**：跟踪气象预警更新，联合技术团队研判次生灾害风险，指导基层防御。
           - **行业部门分管负责人**：依据气象预警类型，落实本行业专项处置措施，做好应急支撑与风险防范。
           - **技术支撑单位分区负责人**：现场开展次生灾害隐患排查与监测，提供技术指导，协助开展灾害损失预判。

        2. **乡镇级响应人员**

           - **乡分管责任人**：统筹本乡镇防御工作，快速传达预警信息，组织高风险区域群众转移，实时上报信息，规范管理临时安置点及应急物资。
           - **地质灾害管理员**：实时接收并传递气象预警更新，汇总上报辖区气象影响情况与隐患信息。
           - **技术单位协管员**：对接县级技术部门，反馈辖区实况，指导网格级人员开展科学防御。

        3. **村级响应人员**

           - **专管员**：及时传达气象预警信息，组织群众排查转移，上报信息，开展隐患巡查与物资分发。
           - **风险区行业部门联防员**：针对气象预警可能引发的行业风险，排查隐患，监测风险变化，及时上报并协同处置。

        4. **网格级响应人员**

           - **监测员**：按频次（{orange_monitor_frequency}，如：每8小时1次）监测风险点，异常情况及时上报并协助转移。
           - **风险区巡查员**：每日至少开展 {orange_patrol_times} 次（如：3次）巡查，重点排查易受气象灾害影响区域，排查隐患并协助避险。
        """;

    private static final String REG_PLAN_YELLOW_SECTION = """
        #### （一）黄色响应：人员职责

        1. **县级响应人员**

           - **分管县长**：统筹处置工作，传达气象预警信息，部署重点防御任务，掌握乡镇进展，协调部门提供应急支撑。
           - **自然资源部门分管负责人**：跟踪气象预警更新，组织技术团队研判次生灾害风险，指导基层工作。
           - **行业部门分管负责人**：依据气象预警类型，落实本行业处置措施，做好隐患排查与应急支撑。
           - **技术支撑单位分区负责人**：开展现场隐患排查与监测，指导基层防治工作，防范衍生风险。

        2. **乡镇级响应人员**

           - **乡分管责任人**：落实县级要求，传达预警信息，组织高风险区域群众转移，及时上报进展信息，保障应急物资与安置点管理。
           - **地质灾害管理员**：接收并传递气象预警更新，汇总上报监测数据与隐患信息。
           - **技术单位协管员**：对接县级技术部门，反馈辖区实况，指导网格级工作。

        3. **村级响应人员**

           - **专管员**：传达气象预警信息，组织群众排查转移，上报信息，开展隐患巡查与物资分发。
           - **风险区行业部门联防员**：针对气象预警可能引发的行业风险，排查隐患，监测风险变化，及时上报并协同处置。

        4. **网格级响应人员**

           - **监测员**：按频次（{yellow_monitor_frequency}，如：每12小时1次）监测风险点，异常情况及时上报并协助转移。
           - **风险区巡查员**：每日开展 {yellow_patrol_times} 次巡查，重点排查易受气象灾害影响区域，排查隐患并协助避险。
        """;

    private static final String REG_PLAN_BLUE_SECTION = """
        #### （一）蓝色响应：人员职责

        1. **县级响应人员**

           - **分管县长**：统筹处置工作，传达气象预警信息，掌握乡镇进展，协调部门做好指导支撑。
           - **自然资源部门分管负责人**：跟踪气象预警更新，组织技术指导，汇总上报信息。
           - **技术支撑单位分区负责人**：提供技术指导，协助基层开展隐患排查与风险研判，提出防范建议。
           - **行业部门分管负责人（水利/交通等）**：跟踪气象预警更新，组织行业内技术指导，汇总上报信息。

        2. **乡镇级响应人员**

           - **行业部门分管负责人（水利、交通、住建、民政等）**：按职责指导本行业结合气象预警加强关注，做好应急处置准备。
           - **乡分管责任人**：落实县级指导要求，传达预警信息，关注实时情况。
           - **地质灾害管理员**：接收并传递气象预警更新，关注实时情况。
           - **技术单位协管员**：协助技术监测与隐患识别，传递指导信息并反馈基层情况。

        3. **村级响应人员**

           - **专管员**：传达气象预警信息，关注天气变化，及时上报信息，必要时组织群众转移。
           - **风险区行业部门联防员**：针对气象预警可能引发的行业风险，关注风险变化，及时上报并协助防范应对。

        4. **网格级响应人员**

           - **监测员**：接收气象预警信息，关注天气变化，必要时进行巡查，发现异常及时上报异常并做好防范宣传。
           - **风险区巡查员**：接收气象预警信息，关注天气变化，必要时进行巡查，发现异常时排查隐患并协助群众防范应对。
        """;

    private static final String REG_PLAN_TEMPLATE_SUFFIX = """
        ### 四、保障措施

        #### （一）信息报送保障

        建立四级信息报送机制，按时限（Ⅰ级、Ⅱ级响应每30分钟上报1次气象预警更新、灾害影响及处置进展，Ⅲ级响应每1小时上报1次，Ⅳ级响应每2小时上报1次）上报信息，确保气象数据、隐患情况、处置措施等信息及时准确完整，严禁迟报、漏报、瞒报。由县政府统筹、气象部门牵头落实。

        #### （二）物资与队伍保障

        县政府统筹应急队伍与物资，针对不同等级气象预警可能引发的灾害类型，储备专项应急物资（如防汛沙袋、救生衣、除雪设备等），明确储备地点与调配流程；气象部门协助提供预警信息支撑，乡、村、网格级做好物资储备管理与分发，确保应急调用高效。

        #### （三）培训与演练保障

        县级每年组织至少2次针对性培训演练，聚焦气象预警识别、次生灾害防范、应急处置流程等内容，由县政府统筹、气象部门组织；乡、村级每半年开展专项培训演练，提升快速响应与协同处置能力。

        #### （四）责任追究保障

        对严格落实气象预警响应要求、履职尽责成效显著者予以表彰；对因预警传达不及时、防御措施不到位、处置不当导致灾害损失扩大的，依法依规追究责任。

        ### 五、附则

        - 本方案由 {explain_unit}负责解释。
        - 本方案根据 {revision_basis_area}气象灾害特点及预警技术发展，由县政府牵头、气象部门负责修订完善。
        - 本方案自发布之日起施行。

        **编制单位**：{compile_unit}联合编制
        **编制日期**：{compile_date}
        """;

    public static final String KEY_DEFENSE_AREA = "defense_area";

    /**
     * 恩施市整体等级描述（如 处于3级）
     */
    public static final String KEY_DEFAULT_LEVEL_TEXT = "default_level_text";
    /**
     * 响应等级文案（如 气象预警红色级）
     */
    public static final String KEY_RESPONSE_LEVEL_TEXT = "response_level_text";
    /**
     * 启动时间（如 2026年02月24日15时00分）
     */
    public static final String KEY_START_TIME = "start_time";
    /**
     * 启动条件（如 接收到省气象局发布的地质灾害气象风险红色预警，未来6小时降雨量≥200mm）
     */
    public static final String KEY_TRIGGER_CONDITION = "trigger_condition";
    /**
     * 区域名称（如 XX市XX县XX乡镇）
     */
    public static final String KEY_SPECIFIC_AREA_NAME = "specific_area_name";
    /**
     * 红色响应-监测频次（如 每1小时1次）
     */
    public static final String KEY_RED_MONITOR_FREQUENCY = "red_monitor_frequency";
    /**
     * 红色响应-每日巡查次数（如 4）
     */
    public static final String KEY_RED_PATROL_TIMES = "red_patrol_times";
    /**
     * 橙色响应-监测频次（如 每2小时1次）
     */
    public static final String KEY_ORANGE_MONITOR_FREQUENCY = "orange_monitor_frequency";
    /**
     * 橙色响应-每日巡查次数（如 3）
     */
    public static final String KEY_ORANGE_PATROL_TIMES = "orange_patrol_times";
    /**
     * 黄色响应-监测频次（如 每12小时1次）
     */
    public static final String KEY_YELLOW_MONITOR_FREQUENCY = "yellow_monitor_frequency";
    /**
     * 黄色响应-每日巡查次数（如 2）
     */
    public static final String KEY_YELLOW_PATROL_TIMES = "yellow_patrol_times";
    /**
     * 蓝色响应-监测频次（如 每12小时1次）
     */
    public static final String KEY_BLUE_MONITOR_FREQUENCY = "blue_monitor_frequency";
    /**
     * 蓝色响应-每日巡查次数（如 2）
     */
    public static final String KEY_BLUE_PATROL_TIMES = "blue_patrol_times";
    /**
     * 解释单位（如 XX县人民政府）
     */
    public static final String KEY_EXPLAIN_UNIT = "explain_unit";
    /**
     * 修订依据区域（如 XX县）
     */
    public static final String KEY_REVISION_BASIS_AREA = "revision_basis_area";
    /**
     * 编制单位（如 XX县人民政府、XX县自然资源局联合编制）
     */
    public static final String KEY_COMPILE_UNIT = "compile_unit";
    /**
     * 编制日期（如 2026年02月24日）
     */
    public static final String KEY_COMPILE_DATE = "compile_date";

    /**
     * 按乡镇实际防御响应等级填充区域防御方案模板。
     * 存在某等级的乡镇时，拼接对应 REG_PLAN_*_SECTION，最后追加 REG_PLAN_TEMPLATE_SUFFIX。
     */
    public static String getFilledRegPlanContent(Map<String, String> params, Set<Integer> activeLevels) {
        Set<Integer> levels = normalizeActiveLevels(activeLevels);
        params.putIfAbsent(KEY_DEFAULT_LEVEL_TEXT, resolveDefaultLevelText(levels));
        params.putIfAbsent(KEY_RESPONSE_LEVEL_TEXT, resolveResponseLevelText(levels));
        String template = REG_PLAN_TEMPLATE_PREFIX
            + resolveRegPlanLevelSections(levels)
            + REG_PLAN_TEMPLATE_SUFFIX;
        return fillTemplate(template, params);
    }

    private static Set<Integer> normalizeActiveLevels(Set<Integer> activeLevels) {
        if (activeLevels == null || activeLevels.isEmpty()) {
            return Set.of(1);
        }
        return activeLevels.stream()
                           .filter(Objects::nonNull)
                           .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
    }

    private static final String[] SECTION_CN_NUMBERS = {"一", "二", "三", "四"};

    private static String resolveRegPlanLevelSections(Set<Integer> levels) {
        StringBuilder section = new StringBuilder();
        int index = 0;
        if (levels.contains(4)) {
            section.append(renumberSectionTitle(REG_PLAN_RED_SECTION, SECTION_CN_NUMBERS[index++]));
        }
        if (levels.contains(3)) {
            section.append(renumberSectionTitle(REG_PLAN_ORANGE_SECTION, SECTION_CN_NUMBERS[index++]));
        }
        if (levels.contains(2)) {
            section.append(renumberSectionTitle(REG_PLAN_YELLOW_SECTION, SECTION_CN_NUMBERS[index++]));
        }
        if (levels.contains(1)) {
            section.append(renumberSectionTitle(REG_PLAN_BLUE_SECTION, SECTION_CN_NUMBERS[index++]));
        }
        return section.isEmpty() ? renumberSectionTitle(REG_PLAN_BLUE_SECTION, SECTION_CN_NUMBERS[0]) : section.toString();
    }

    private static String renumberSectionTitle(String section, String cnNumber) {
        return section.replaceFirst("#### （一）", "#### （" + cnNumber + "）");
    }

    private static String resolveDefaultLevelText(Set<Integer> levels) {
        Integer highestLevel = levels.stream().min(Integer::compareTo).orElse(1);
        return "处于" + resolveRomanLevelText(highestLevel);
    }

    private static String resolveResponseLevelText(Set<Integer> levels) {
        if (levels.size() == 1) {
            return "全部乡镇处于" + resolveRomanLevelText(levels.iterator().next());
        }
        Integer highestLevel = levels.stream().min(Integer::compareTo).orElse(1);
        return "全部乡镇处于" + resolveRomanLevelText(highestLevel);
    }

    private static String resolveRomanLevelText(Integer normalizedLevel) {
        return Objects.toString(LevelCodeUtil.resolveDefenseResponseRomanLevel(normalizedLevel), "Ⅳ级");
    }

    /**
     * 通用模板填充实现。
     */
    private static String fillTemplate(String template, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return template;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder(template.length() + 256);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = params.get(key);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(Objects.toString(value, "")));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private DefRespPlanTemplateContent() {
        // 常量类，禁止实例化
    }
}
