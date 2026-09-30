/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RiskWarningRecordBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyWarningDisposalBo;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorDevice;
import cn.edu.pku.whai.geological.disaster.data.domain.po.MonitorPoint;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.MonitorWarningRecordResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyWarningDisposalVo;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalStatusLookup;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalWarningEventPageResult;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalWarningEventRow;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.LocalWarningLookup;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.MonitorWarningDataResult;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.ThirdMonitorDeviceWarningPageResp;
import cn.edu.pku.whai.geological.disaster.service.thirdparty.warning.dto.ThirdMonitorDeviceWarningResp;
import cn.hutool.core.util.StrUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MonitorWarningDelegate {
    private final ThirdPartyWarningDataSplitSupport thirdPartyWarningDataSplitSupport;

    public MonitorWarningDelegate(ThirdPartyWarningDataSplitSupport thirdPartyWarningDataSplitSupport) {
        this.thirdPartyWarningDataSplitSupport = thirdPartyWarningDataSplitSupport;
    }

    /**
     * 获取监测预警数据（先获取全部数据，然后在内存中分页）
     *
     * @param bo        查询条件，包含开始时间和结束时间
     * @param pageQuery 分页查询参数
     * @return 分页的监测预警数据列表
     */
    TableDataInfo<MonitorWarningRecordResp> getMonitorWarning(RiskWarningRecordBo bo, PageQuery pageQuery) {
        thirdPartyWarningDataSplitSupport.getWarningQueryGuardDelegate().validateParams(bo);
        MonitorWarningDataResult dataResult = thirdPartyWarningDataSplitSupport.fetchMonitorWarningData(bo);
        List<MonitorWarningRecordResp> allRecords = dataResult.getRecords();
        Integer totalCount = dataResult.getTotal();
        List<MonitorWarningRecordResp> pagedRecords = thirdPartyWarningDataSplitSupport.paginateInMemory(allRecords, thirdPartyWarningDataSplitSupport.resolvePageNum(pageQuery), thirdPartyWarningDataSplitSupport.resolvePageSize(pageQuery));
        TableDataInfo<MonitorWarningRecordResp> result = TableDataInfo.build(pagedRecords);
        result.setTotal(totalCount != null && totalCount > 0 ? totalCount : allRecords.size());
        return result;
    }

    /**
     * 获取三方预警处置分页列表
     */
    TableDataInfo<ThirdPartyWarningDisposalVo> getMonitorWarningDisposal(ThirdPartyWarningDisposalBo bo, PageQuery pageQuery) {
        ThirdPartyWarningDisposalBo queryBo = bo == null ? new ThirdPartyWarningDisposalBo() : bo;
        if (StrUtil.isBlank(queryBo.getRegionCode())) queryBo.setRegionCode(ThirdPartyWarningDataSplitSupport.DEFAULT_REGION_CODE);
        thirdPartyWarningDataSplitSupport.getWarningQueryGuardDelegate().validateMonitorWarningDisposalParams(queryBo);
        int pageNum = thirdPartyWarningDataSplitSupport.resolvePageNum(pageQuery);
        int pageSize = thirdPartyWarningDataSplitSupport.resolvePageSize(pageQuery);
        List<MonitorPoint> localMonitorPoints = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().queryLocalMonitorPoints(queryBo);
        if (localMonitorPoints.isEmpty()) return TableDataInfo.build(new ArrayList<ThirdPartyWarningDisposalVo>());
        Map<String, MonitorPoint> pointById = thirdPartyWarningDataSplitSupport.buildMonitorPointById(localMonitorPoints);
        Map<String, MonitorPoint> pointByName = thirdPartyWarningDataSplitSupport.buildMonitorPointByName(localMonitorPoints);
        LocalWarningEventPageResult pageResult = thirdPartyWarningDataSplitSupport.queryLocalWarningEventPage(queryBo, pointById.keySet(), pointByName.keySet(), pageNum, pageSize);
        Map<String, MonitorDevice> deviceById = thirdPartyWarningDataSplitSupport.loadMonitorDeviceById(pointById.keySet());
        Map<String, MonitorDevice> deviceByClientId = thirdPartyWarningDataSplitSupport.buildMonitorDeviceByClientId(deviceById.values());
        LocalStatusLookup statusLookup = thirdPartyWarningDataSplitSupport.loadLocalStatusLookup(new ArrayList<MonitorDevice>(deviceById.values()));
        LocalWarningLookup warningLookup = thirdPartyWarningDataSplitSupport.loadLocalWarningLookup(new ArrayList<MonitorDevice>(deviceById.values()));
        String fallbackRegionName = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().resolveRegionNameByCode(queryBo.getRegionCode());
        List<ThirdPartyWarningDisposalVo> records = new ArrayList<ThirdPartyWarningDisposalVo>();
        for (LocalWarningEventRow row : pageResult.rows()) {
            MonitorPoint mp = thirdPartyWarningDataSplitSupport.resolveLocalMonitorPoint(pointById, pointByName, row.monitorPointId(), row.monitorPointName());
            MonitorDevice device = thirdPartyWarningDataSplitSupport.resolveWarningDevice(row, deviceById, deviceByClientId);
            ThirdPartyWarningDisposalVo vo = new ThirdPartyWarningDisposalVo();
            vo.setWarningId(row.warningId());
            vo.setMonitorPointName(StrUtil.blankToDefault(row.monitorPointName(), mp == null ? null : mp.getMonitorName()));
            vo.setAdministrativeRegion(thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().resolveAdministrativeRegion(mp, fallbackRegionName));
            vo.setLatitude(mp == null ? null : mp.getLatitude());
            vo.setLongitude(mp == null ? null : mp.getLongitude());
            vo.setDeviceId(thirdPartyWarningDataSplitSupport.resolveWarningDeviceId(row, deviceByClientId));
            vo.setWarningDeviceName(thirdPartyWarningDataSplitSupport.resolveWarningDeviceName(row, deviceById, deviceByClientId));
            vo.setWarningLevel(row.warningLevel());
            vo.setWarningPublishTime(thirdPartyWarningDataSplitSupport.formatTimestamp(row.warningTime()));
            vo.setDisposalType(row.disposalType());
            vo.setDisposalPerson(row.disposalPerson());
            vo.setWarningDisposalTime(thirdPartyWarningDataSplitSupport.formatTimestamp(row.disposalTime()));
            vo.setValidWarning(row.validWarning());
            thirdPartyWarningDataSplitSupport.enrichMonitorWarningDisposalRuntimeFields(vo, device, thirdPartyWarningDataSplitSupport.resolveLocalStatus(device, statusLookup), thirdPartyWarningDataSplitSupport.resolveLatestWarningInfo(device, warningLookup), thirdPartyWarningDataSplitSupport.formatTimestamp(row.warningTime()));
            records.add(vo);
        }
        TableDataInfo<ThirdPartyWarningDisposalVo> result = TableDataInfo.build(records);
        result.setTotal(pageResult.total());
        return result;
    }

    TableDataInfo<ThirdPartyWarningDisposalVo> getMonitorWarningDisposalFromRemote(ThirdPartyWarningDisposalBo bo, PageQuery pageQuery) {
        return getMonitorWarningDisposalFromRemote(bo, pageQuery, null, null);
    }

    TableDataInfo<ThirdPartyWarningDisposalVo> getMonitorWarningDisposalFromRemote(
            ThirdPartyWarningDisposalBo bo, PageQuery pageQuery,
            LocalDateTime warningStartTime, LocalDateTime warningEndTime) {
        ThirdPartyWarningDisposalBo queryBo = bo == null ? new ThirdPartyWarningDisposalBo() : bo;
        queryBo.setRegionCode(ThirdPartyWarningDataSplitSupport.DEFAULT_REGION_CODE);
        thirdPartyWarningDataSplitSupport.getWarningQueryGuardDelegate().validateMonitorWarningDisposalParams(queryBo);
        int pageNum = thirdPartyWarningDataSplitSupport.resolvePageNum(pageQuery);
        int pageSize = thirdPartyWarningDataSplitSupport.resolvePageSize(pageQuery);
        LocalDateTime queryEndTime = warningEndTime == null ? LocalDateTime.now(ThirdPartyWarningDataSplitSupport.BEIJING_ZONE_ID) : warningEndTime;
        LocalDateTime queryStartTime = warningStartTime == null ? thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().resolveRemoteWarningStartTime(queryBo, queryEndTime) : warningStartTime;
        ThirdMonitorDeviceWarningPageResp pageResp = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().fetchMonitorDeviceWarningPage(null, "", ThirdPartyWarningDataSplitSupport.DEFAULT_REGION_CODE, queryBo.getWarningLevel(), queryBo.getValidWarning(), queryBo.getDisposalType(), pageNum, pageSize, queryStartTime, queryEndTime);
        ThirdPartyWarningDisposalBo localBo = new ThirdPartyWarningDisposalBo();
        localBo.setRegionCode(ThirdPartyWarningDataSplitSupport.DEFAULT_REGION_CODE);
        List<MonitorPoint> localMonitorPoints = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().queryLocalMonitorPoints(localBo);
        Map<String, MonitorPoint> pointById = thirdPartyWarningDataSplitSupport.buildMonitorPointById(localMonitorPoints);
        Map<String, MonitorPoint> pointByName = thirdPartyWarningDataSplitSupport.buildMonitorPointByName(localMonitorPoints);
        Map<String, MonitorPoint> localMonitorPointMap = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().buildLocalMonitorPointMap(localMonitorPoints);
        Map<String, MonitorDevice> deviceById = thirdPartyWarningDataSplitSupport.loadMonitorDeviceById(pointById.keySet());
        List<MonitorDevice> devices = new ArrayList<MonitorDevice>(deviceById.values());
        Map<String, String> localDeviceIdByPointAndName = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().buildMonitorDeviceIdByPointAndName(devices);
        Map<String, List<MonitorDevice>> devicesByPointId = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().buildMonitorDevicesByPointId(devices);
        LocalStatusLookup statusLookup = thirdPartyWarningDataSplitSupport.loadLocalStatusLookup(devices);
        LocalWarningLookup warningLookup = thirdPartyWarningDataSplitSupport.loadLocalWarningLookup(devices);
        String fallbackRegionName = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().resolveRegionNameByCode(ThirdPartyWarningDataSplitSupport.DEFAULT_REGION_CODE);
        List<ThirdPartyWarningDisposalVo> records = new ArrayList<ThirdPartyWarningDisposalVo>();
        if (pageResp != null && pageResp.getRecords() != null) {
            for (ThirdMonitorDeviceWarningResp wr : pageResp.getRecords()) {
                MonitorPoint mp = thirdPartyWarningDataSplitSupport.resolveRemoteLocalMonitorPoint(wr, localMonitorPointMap, pointById, pointByName);
                MonitorDevice device = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().resolveWarningDevice(wr, mp, deviceById, localDeviceIdByPointAndName, devicesByPointId);
                ThirdPartyWarningDisposalVo vo = thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().buildMonitorWarningDisposalVo(wr, mp, fallbackRegionName);
                thirdPartyWarningDataSplitSupport.enrichMonitorWarningDisposalRuntimeFields(vo, device, thirdPartyWarningDataSplitSupport.resolveLocalStatus(device, statusLookup), thirdPartyWarningDataSplitSupport.resolveLatestWarningInfo(device, warningLookup), wr == null ? null : wr.getYjsj());
                if (thirdPartyWarningDataSplitSupport.getWarningDisposalDelegate().matchRemoteMonitorWarningDisposal(queryBo, vo)) records.add(vo);
            }
        }
        TableDataInfo<ThirdPartyWarningDisposalVo> result = TableDataInfo.build(records);
        result.setTotal(pageResp != null && pageResp.getTotal() != null ? pageResp.getTotal() : records.size());
        return result;
    }
}
