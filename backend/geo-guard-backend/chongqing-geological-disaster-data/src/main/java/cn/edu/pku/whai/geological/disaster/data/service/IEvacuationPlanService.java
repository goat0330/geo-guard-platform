/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.EvacuationPlanBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.EvacuationPlan;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.EvacuationPlanRouteResultVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.EvacuationPlanVo;

import java.util.List;

/**
 * 撤离方案表 Service 接口
 *
 * @author whai
 * @date 2026-02-04
 */
public interface IEvacuationPlanService {

    /**
     * 按主键查询
     *
     * @param id 主键
     * @return 撤离方案 VO
     */
    EvacuationPlanVo queryById(Long id);

    /**
     * 根据方案ID查询
     *
     * @param handleId 方案Id
     * @return 撤离方案 VO
     */
    List<EvacuationPlan> listByPlanId(Long handleId);

    /**
     * 根据handleId查询当前方案下的所有撤离路线
     *
     * @param handleId 处置主键
     * @return 撤离路线查询结果
     */
    EvacuationPlanRouteResultVo queryRoutesByHandleId(Long handleId);

    /**
     * 分页查询
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    TableDataInfo<EvacuationPlanVo> queryPageList(EvacuationPlanBo bo, PageQuery pageQuery);

    /**
     * 列表查询（不分页）
     *
     * @param bo 查询条件
     * @return 列表
     */
    List<EvacuationPlanVo> queryList(EvacuationPlanBo bo);
}
