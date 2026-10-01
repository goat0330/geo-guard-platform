/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;


import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RiskZoneBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RiskZoneStatVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RiskZoneVo;

import java.util.List;

/**
 * 风险区Service接口
 *
 * @author lizheng
 * @date 2026-01-10
 */
public interface IRiskZoneService {

    /**
     * 查询风险区
     *
     * @param id 主键
     * @return 风险区
     */
    RiskZoneVo queryById(String id);

    /**
     * 分页查询风险区列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 风险区分页列表
     */
    TableDataInfo<RiskZoneVo> queryPageList(RiskZoneBo bo, PageQuery pageQuery);

    List<RiskZoneVo> queryByArea(String area);

    List<RiskZoneStatVo> statRiskLevel();
}
