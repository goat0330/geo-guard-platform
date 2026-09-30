package cn.edu.pku.whai.geological.disaster.service.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzTaskHandleApprovalBo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleApprovalVo;

import java.util.Collection;
import java.util.List;

/**
 * 任务处置审批Service接口
 *
 * @author kongweiguang
 * @date 2026-02-06
 */
public interface IDzTaskHandleApprovalService {

    /**
     * 查询任务处置审批
     *
     * @param id 主键
     * @return 任务处置审批
     */
    DzTaskHandleApprovalVo queryById(Long id);

    /**
     * 分页查询任务处置审批列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 任务处置审批分页列表
     */
    TableDataInfo<DzTaskHandleApprovalVo> queryPageList(DzTaskHandleApprovalBo bo, PageQuery pageQuery);

    /**
     * 查询符合条件的任务处置审批列表
     *
     * @param bo 查询条件
     * @return 任务处置审批列表
     */
    List<DzTaskHandleApprovalVo> queryList(DzTaskHandleApprovalBo bo);

    /**
     * 新增任务处置审批
     *
     * @param bo 任务处置审批
     * @return 是否新增成功
     */
    Boolean insertByBo(DzTaskHandleApprovalBo bo);

    /**
     * 修改任务处置审批
     *
     * @param bo 任务处置审批
     * @return 是否修改成功
     */
    Boolean updateByBo(DzTaskHandleApprovalBo bo);

    /**
     * 校验并批量删除任务处置审批信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 获取指定轮次的任务处置审批列表
     *
     * @param handleId 处置任务id
     * @param roundNo  轮次；为空时由实现决定默认轮次
     * @return 任务处置审批列表
     */
    List<DzTaskHandleApprovalVo> listStatus(Long handleId, Integer roundNo);
}
