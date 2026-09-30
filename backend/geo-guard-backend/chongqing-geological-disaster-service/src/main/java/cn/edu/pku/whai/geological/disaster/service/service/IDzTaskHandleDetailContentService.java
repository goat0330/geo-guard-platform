/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleDetailContentBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleDetailContentVo;

import java.util.Collection;
import java.util.List;

/**
 * 处置管理详情内容服务。
 */
public interface IDzTaskHandleDetailContentService {

    /**
     * 查询处置管理详情内容。
     *
     * @param id 主键
     * @return 处置管理详情内容
     */
    DzTaskHandleDetailContentVo queryById(Long id);

    /**
     * 分页查询处置管理详情内容列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 处置管理详情内容分页列表
     */
    TableDataInfo<DzTaskHandleDetailContentVo> queryPageList(DzTaskHandleDetailContentBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的处置管理详情内容列表。
     *
     * @param bo 查询条件
     * @return 处置管理详情内容列表
     */
    List<DzTaskHandleDetailContentVo> queryList(DzTaskHandleDetailContentBo bo);

    /**
     * 查询指定业务下标记为最新的一条详情内容。
     *
     * @param bizType     业务类型
     * @param bizId       业务主键
     * @param contentType 内容类型
     * @return 最新详情内容
     */
    DzTaskHandleDetailContentVo queryLatest(Integer bizType, Long bizId, Integer contentType, Integer roundNo);

    /**
     * 新增处置管理详情内容。
     *
     * @param bo 处置管理详情内容
     * @return 是否新增成功
     */
    Boolean insertByBo(DzTaskHandleDetailContentBo bo);

    /**
     * 修改处置管理详情内容。
     *
     * @param bo 处置管理详情内容
     * @return 是否修改成功
     */
    Boolean updateByBo(DzTaskHandleDetailContentBo bo);

    /**
     * 保存或更新防御响应会商确认内容（同轮次待提交记录存在则更新，否则新增）。
     *
     * @param bo 会商确认内容
     * @return 详情内容主键
     */
    Long saveConsultationConfirmByBo(DzTaskHandleDetailContentBo bo);

    /**
     * 查询指定轮次下气象预警同步的会商确认记录。
     */
    DzTaskHandleDetailContentVo queryLatestAlarmSyncedConsultation(Long defId, Integer roundNo);

    /**
     * 查询指定轮次下待审批的会商确认记录。
     */
    DzTaskHandleDetailContentVo querySubmittedConsultationDetail(Long defId, Integer roundNo);

    DzTaskHandleDetailContentVo queryLatestPendingFinalReport(Long defId, Integer roundNo);

    /**
     * 查询指定轮次下待审批的会商确认记录，不存在时抛异常。
     */
    DzTaskHandleDetailContentVo requireLatestPendingConsultationDetail(Long defId, Integer roundNo);

    /**
     * 保存或更新气象预警同步的会商确认内容。
     */
    Long saveAlarmSyncedConsultation(Long defId, Integer roundNo, String planContentJson);

    /**
     * 插入初报详情内容；若已有会商确认则优先用其内容组装。仅由县级方案创建时触发。
     */
    void insertInitialReportContent(Long defId, String content, Integer roundNo);

    /**
     * 插入终报详情内容；若已有会商确认则优先用其内容。
     */
    void insertInitialAndFinalReportContent(Long defId, String content, Integer roundNo);

    /**
     * 以已提交会商确认内容为模板，新增终报详情内容。
     */
    void insertInitialAndFinalReportFromSubmittedConsultation(DzTaskHandleDetailContentVo submittedConsultation, Integer roundNo);

    /**
     * 将会商确认记录状态更新为已审批。
     */
    Boolean updateConsultationConfirmApproved(DzTaskHandleDetailContentVo detailContent);

    /**
     * 将预警同步状态的会商确认记录重置为待提交。
     */
    void resetAlarmSyncedConsultationToPendingSubmit(Long defId);

    /**
     * 校验并批量删除处置管理详情内容信息。
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    void saveEvacuationPlanContent(Long handleId, Integer roundNo, String planContent, String planContentJson);

    void saveEvacuationPlanContent(Long handleId, Integer roundNo, String planContent, String planContentJson, Long userId);

    void saveEvacuationPlanContent(Long handleId, Integer roundNo, String planContent, String planContentJson, Long userId, String userName);

    void saveDefRespPlanContent(Long defId, Integer roundNo, String planContent);

    void saveAiReportContent(Long handleId, Integer roundNo, String reportContent);

    void saveAiReportContent(Long handleId, Integer roundNo, String reportContent, Long userId, String userName);

    void saveAiReportContent(Long handleId, Integer roundNo, String reportContent, String planContentJson, Long userId, String userName);

    void saveReviewReportContent(Long handleId, Integer roundNo, String reportContent);

    void saveReviewReportContent(Long handleId, Integer roundNo, String reportContent, Long userId, String userName);

    void updateLatestSuggest(Integer bizType, Long bizId, Integer contentType, String suggest);

    void markLatestById(Long id);
}
