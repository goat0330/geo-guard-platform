/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;


import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.MonitorPointBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.HazardPointVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorPointVo;

import java.util.List;

/**
 * 监测点基本情况Service接口
 *
 * @author lizheng
 * @date 2026-01-06
 */
public interface IMonitorPointService {

    /**
     * 查询监测点基本情况
     *
     * @param id 主键
     * @return 监测点基本情况
     */
    MonitorPointVo queryById(String id);

    /**
     * 分页查询监测点基本情况列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 监测点基本情况分页列表
     */
    TableDataInfo<MonitorPointVo> queryPageList(MonitorPointBo bo, PageQuery pageQuery);

    /**
     * 根据范围WKT查询监测点列表
     *
     * @param wkt 范围WKT
     * @return 监测点列表
     */
    List<MonitorPointVo> queryByWkt(String wkt);

    /**
     * 根据监测点名称列表查询监测点
     *
     * @param monitorPointNames 监测点名称列表
     * @return 监测点列表
     */
    List<MonitorPointVo> queryByMonitorNames(List<String> monitorPointNames);


    /**
     * 从接口同步数据
     */
    void syncData();

    List<MonitorPointVo> queryBySlopeUnitId(String id);
}
