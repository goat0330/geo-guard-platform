/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.DefRespRangeResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataAlarmVo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanApprovalStatusBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleDetailContentBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanBatchRelateBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanGenerateBo;
import cn.edu.pku.whai.geological.disaster.service.domain.dto.DefRespPlanDto;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespPlanChildGeoAdviceVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespPlanRangeSlopeUnitVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespPlanStreetGeoAdviceVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespStartSmsPreviewVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespPlanVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DefRespTownStatVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.SmsSendSummaryVo;
import cn.hutool.core.lang.tree.Tree;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IDzDefRespPlanService {

    DefRespPlanVo queryById(Long id);

    TableDataInfo<DefRespPlanVo> listByCondition(DefRespPlanDto bo, PageQuery pageQuery);

    DefRespTownStatVo getCirculatingTownStats(Long id);

    Boolean insertByBo(DefRespPlanBo bo);

    R<Map<String, Object>> updateByBo(DefRespPlanBo bo);

    R<String> updateApprovalStatusByBo(DefRespPlanApprovalStatusBo bo);

    R<Map<String, Object>> processNext(Long id);

    String generateLatestPlanContent(DefRespPlanGenerateBo bo);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    List<Tree<String>> getTree();

    DefRespRangeResp getDefenseRespRange();

    List<DefRespPlanRangeSlopeUnitVo> getRangeSlopeUnits(Long defId);

    List<DefRespPlanChildGeoAdviceVo> listChildGeoAdvice(Long alarmId);

    /**
     * 按乡镇/街道列表查询地象建议。
     *
     * @param streets 乡镇/街道名称列表，不能为空
     * @return 与入参顺序一致的地象建议结果列表
     */
    List<DefRespPlanStreetGeoAdviceVo> queryStreetGeoAdvice(List<String> streets);

    Map<String, Object> startSingle(Long handleIdLong);

    R<String> insertExecutiveByBo(Long handleId, Integer meetingType, Integer handleProcess, Integer roundNo);

    /**
     * 预警驱动更新「县级」区域防御响应（非手工状态机）。
     * <ul>
     *   <li>范围缩小：关闭对应乡镇行及开放任务</li>
     *   <li>范围扩大：为新增街道插入乡镇行</li>
     *   <li>县级等级创建后不可变，若预警等级不同应改为关联或创建对应等级的县级行</li>
     *   <li>最后写回县级 streets/trigger，并将县级 status 置为方案生成</li>
     * </ul>
     */
    DefRespPlan updateCountyRegionFromAlarm(DefRespPlanVo countyVo, DataAlarmVo dataAlarmVo);

    /**
     * 按 city + streets + level 增量生成或更新区域防御响应：复用同县同等级的有效县级行，仅为入参街道维护乡镇行；
     * 未出现在入参中的乡镇保持不变。若县级关联乡镇集合扩大（新增街道/新建或改挂乡镇行），则轮次+1（原流程已过方案生成时）、
     * 回退至方案生成、重新生成方案，并同步所有有效乡镇行状态。
     */
    DefRespPlan generateOrUpdateRegionPlansFromAlarm(DataAlarmVo dataAlarmVo);

    Long eventCount(AdRegionVo adRegionVo, List<Integer> status);

    List<DefRespPlanVo> getRelationInfo(Long id);

    /**
     * 按多组入参批量更新乡镇级区域防御响应等级，并维护其与县级区域防御响应的关联关系。
     */
    int batchUpdateTownRegionLevels(List<DefRespPlanBatchRelateBo> boList);

    /**
     * 比对入参与当前流转中乡镇级防御响应等级是否完全一致。
     *
     * @param boList 乡镇与目标等级列表
     * @return 完全一致返回 1，否则返回 0
     */
    int matchCirculatingTownLevels(List<DefRespPlanBatchRelateBo> boList);

    /**
     * 保存防御响应会商确认内容，提交待审批时同步维护县级防御响应。
     */
    Long saveConsultationConfirm(DzTaskHandleDetailContentBo bo);

    void handleRegionApprovalPassed(Long handleId, Long approvalId);

    /**
     * 按指定气象预警解析乡镇与等级并保存会商确认内容。
     *
     * @param defId   县级区域防御响应方案 id
     * @param alarmId 气象预警 id
     * @return 会商确认详情内容 id
     */
    Long syncConsultationFromAlarm(Long defId, Long alarmId);

    /**
     * 预览县级区域防御响应正式启动短信。
     */
    List<DefRespStartSmsPreviewVo> previewStartSms(Long defId);

    /**
     * 手动补发县级区域防御响应正式启动短信。
     */
    SmsSendSummaryVo sendStartSms(Long defId);
}
