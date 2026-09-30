package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.AdRegionVo;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.DzTaskHandleReq;
import cn.edu.pku.whai.geological.disaster.service.app.domain.req.DzTaskHandleSceneRecordReq;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.AiModifyReportBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.GenerateReviewReportBo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.ModifyReportBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskOverviewVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleRiskAreaStatVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.EvacuationSmsSendWrapResult;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.HandleStatVo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collection;
import java.util.List;

/**
 * 灾害处置Service接口
 *
 * @author kongweiguang
 * @date 2026-01-29
 */
public interface IDzTaskHandleService {

    /**
     * 查询灾害处置
     *
     * @param id 主键
     * @return 灾害处置
     */
    DzTaskHandleVo queryById(Long id);

    /**
     * 分页查询灾害处置列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 灾害处置分页列表
     */
    TableDataInfo<DzTaskHandleVo> queryPageList(DzTaskHandleBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的灾害处置列表
     *
     * @param bo 查询条件
     * @return 灾害处置列表
     */
    List<DzTaskHandleVo> queryList(DzTaskHandleBo bo);

    /**
     * 新增灾害处置
     *
     * @param bo 灾害处置
     * @return 是否新增成功
     */
    Boolean insertByBo(DzTaskHandleBo bo);

    /**
     * 修改灾害处置
     *
     * @param bo 灾害处置
     * @return 是否修改成功
     */
    Boolean updateByBo(DzTaskHandleBo bo);

    /**
     * 校验并批量删除灾害处置信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    String aiModifyReport(AiModifyReportBo bo);

    Integer processNext(Long id);

    /**
     * 自动模式推进下一步。
     *
     * @param id 处置任务 id
     * @param actorUserId 自动执行人用户ID，可为空
     * @param actorName 自动执行人名称
     * @return 推进后的处理进度
     */
    Integer autoProcessNext(Long id, Long actorUserId, String actorName);

    /**
     * 现场处置任务反馈后，按处置分支规则尝试自动归档。
     *
     * @param handleId 处置任务 id
     * @param actorUserId 提交人用户ID
     * @param actorName 提交人名称
     * @return 是否实际完成自动归档
     */
    boolean tryAutoArchiveAfterEmergencyTaskFeedback(Long handleId, Long actorUserId, String actorName);


    void modifyReport(ModifyReportBo bo);

    HandleStatVo stat();

    Boolean execute(Long handleId);

    Long insertByReq(DzTaskHandleReq req);

    void addSceneRecord(DzTaskHandleSceneRecordReq req);

    Integer skip(Long id);

    DzRiskOverviewVo riskOverview(Integer type);

    Long eventCount(AdRegionVo adRegionVo, List<Integer> handleProcess);

    void aiModifyReportStream(SseEmitter sseEmitter, AiModifyReportBo bo);

    void generateReviewReportStream(SseEmitter sseEmitter, GenerateReviewReportBo bo);

    List<DzTaskHandleVo> getAllHandling();

    EvacuationSmsSendWrapResult status(Long handleIdLong);

    DzTaskHandleRiskAreaStatVo riskAreaStat(Long handleId);
}
