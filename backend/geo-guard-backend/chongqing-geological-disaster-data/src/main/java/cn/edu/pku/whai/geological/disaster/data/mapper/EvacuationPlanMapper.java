/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import cn.edu.pku.whai.geological.disaster.data.domain.po.EvacuationPlan;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.EvacuationPlanSlopeTerrainVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.EvacuationPlanVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 撤离方案表 Mapper 接口
 *
 * @author whai
 * @date 2026-02-04
 */
public interface EvacuationPlanMapper extends BaseMapperPlus<EvacuationPlan, EvacuationPlanVo> {

    /**
     * 分页查询（撤离路线用 ST_AsText 转为 WKT 字符串）
     *
     * @param page 分页参数
     * @param lqw  查询条件
     * @return 分页结果
     */
    IPage<EvacuationPlanVo> selectEvacuationPlanVoPage(IPage<EvacuationPlanVo> page,
                                                        @Param(Constants.WRAPPER) LambdaQueryWrapper<EvacuationPlan> lqw);

    /**
     * 列表查询（撤离路线用 ST_AsText 转为 WKT 字符串）
     *
     * @param lqw 查询条件
     * @return 列表
     */
    List<EvacuationPlanVo> selectEvacuationPlanVoList(@Param(Constants.WRAPPER) LambdaQueryWrapper<EvacuationPlan> lqw);

    /**
     * 按主键查询（撤离路线用 ST_AsText 转为 WKT 字符串）
     *
     * @param id 主键
     * @return VO
     */
    EvacuationPlanVo selectEvacuationPlanVoById(@Param("id") Long id);

    /**
     * 根据handleId查询斜坡单元坡向、坡度
     *
     * @param handleId 处置主键
     * @return 坡向、坡度
     */
    EvacuationPlanSlopeTerrainVo selectSlopeTerrainByHandleId(@Param("handleId") Long handleId);

    /**
     * 根据handleId删除撤离方案
     *
     * @param handleId 处置主键
     * @return 删除条数
     */
    int deleteByHandleId(@Param("handleId") Long handleId);

    /**
     * 新增撤离方案（evacuation_road以WKT写入geometry）
     *
     * @param plan 撤离方案
     * @return 影响行数
     */
    int insertPlan(@Param("plan") EvacuationPlan plan);
}
