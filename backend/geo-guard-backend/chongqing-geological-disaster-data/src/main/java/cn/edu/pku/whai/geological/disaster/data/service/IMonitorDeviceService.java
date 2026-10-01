/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;


import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.MonitorDeviceBo;
import cn.edu.pku.whai.geological.disaster.data.domain.req.MonitorStatReq;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.MonitorDeviceTreeResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorDeviceStatByHazardVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorDeviceVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorStatVo;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 监测设备基本情况Service接口
 *
 * @author lizheng
 * @date 2026-01-06
 */
public interface IMonitorDeviceService {

    /**
     * 查询监测设备基本情况
     *
     * @param id 主键
     * @return 监测设备基本情况
     */
    MonitorDeviceVo queryById(String id);

    /**
     * 分页查询监测设备基本情况列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 监测设备基本情况分页列表
     */
    TableDataInfo<MonitorDeviceVo> queryPageList(MonitorDeviceBo bo, PageQuery pageQuery);


    /**
     * 从接口同步数据
     */
    void syncData();

    /**
     * 获取监测设备监测数据
     *
     * @param id 主键
     * @return 监测设备树形结构数据
     */
    MonitorDeviceTreeResp queryMonitorDataById(@NotNull(message = "主键不能为空") String id);

    MonitorStatVo stat();

    MonitorStatVo statByArea(MonitorStatReq req);


    List<MonitorDeviceVo> queryBySlopeUnitId(String id);

    /**
     * 按灾害点唯一编号（v_hazard_point.unique_disaster_id = v_monitor_point.basic_info_id）
     * 聚合统计其下监测点的专业监测设备台套、GNSS 台数、裂缝计台数。
     *
     * @param disasterIds 灾害点唯一编号列表
     * @return 统计结果列表（一个灾害点一条）
     */
    List<MonitorDeviceStatByHazardVo> statByDisasterIds(List<String> disasterIds);
}
