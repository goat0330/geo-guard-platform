package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RiskWarningRecordBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.StatThirdWarningBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyDeviceCureBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyDeviceCureDirectBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyMonitorDeviceBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyMonitorDeviceTreeBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyWarningDisposalBo;
import cn.edu.pku.whai.geological.disaster.data.domain.req.warning.FileInfoReq;
import cn.edu.pku.whai.geological.disaster.data.domain.req.warning.RiskWarningRecordReq;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.FileDataResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.FileInfoResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.MonitorWarningRecordResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.RiskWarningRecordPageResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.RiskWarningRecordResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.ThirdPartyWarningDataResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.UserInfoResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DeviceRainfallDisplacementVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.LiveRainVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorDeviceRuntimeVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RiskWarningRecordVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.StatThirdWarningVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyDeviceCureVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyMonitorDeviceTreeVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyMonitorDeviceVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyWarningDisposalVo;
import cn.edu.pku.whai.geological.disaster.data.service.IThirdPartyDeviceCurveQueryService;
import cn.edu.pku.whai.geological.disaster.data.service.IThirdPartyMonitorDeviceService;
import cn.edu.pku.whai.geological.disaster.data.service.IThirdPartyMonitorWarningService;
import cn.edu.pku.whai.geological.disaster.data.service.IThirdPartyWarningDataService;
import cn.edu.pku.whai.geological.disaster.data.service.IThirdPartyWeatherWarningService;
import io.github.kongweiguang.http.client.Req;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 对接第三方气象风险预警预警接口（门面）
 * <p>所有方法委托给 weatherWarningService / monitorWarningService / monitorDeviceService / deviceCurveQueryService。</p>
 *
 * @author kongweiguang
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ThirdPartyWarningDataServiceImpl implements IThirdPartyWarningDataService {

    private final IThirdPartyWeatherWarningService weatherWarningService;
    private final IThirdPartyMonitorWarningService monitorWarningService;
    private final IThirdPartyMonitorDeviceService monitorDeviceService;
    private final IThirdPartyDeviceCurveQueryService deviceCurveQueryService;
    private final ThirdPartyWarningDataSplitSupport splitSupport;

    @Override
    public TableDataInfo<RiskWarningRecordVo> getWeatherWarning(RiskWarningRecordBo bo, PageQuery pageQuery) {
        return weatherWarningService.getWeatherWarning(bo, pageQuery);
    }

    @Override
    public UserInfoResp loginAuth() {
        return weatherWarningService.loginAuth();
    }

    @Override
    public RiskWarningRecordPageResp getRiskWarningRecordPage(RiskWarningRecordReq req) {
        return weatherWarningService.getRiskWarningRecordPage(req);
    }

    @Override
    public RiskWarningRecordResp weatherWarningDetail(String id) {
        return weatherWarningService.weatherWarningDetail(id);
    }

    @Override
    public List<FileInfoResp> getFileInfo(FileInfoReq req) {
        return weatherWarningService.getFileInfo(req);
    }

    @Override
    public FileDataResp getAnalysisResult(String id) {
        return weatherWarningService.getAnalysisResult(id);
    }

    @Override
    public FileDataResp getFileById(String fileId) {
        return weatherWarningService.getFileById(fileId);
    }

    @Override
    public TableDataInfo<MonitorWarningRecordResp> getMonitorWarning(RiskWarningRecordBo bo, PageQuery pageQuery) {
        return monitorWarningService.getMonitorWarning(bo, pageQuery);
    }

    @Override
    public TableDataInfo<ThirdPartyWarningDisposalVo> getMonitorWarningDisposal(ThirdPartyWarningDisposalBo bo, PageQuery pageQuery) {
        return monitorWarningService.getMonitorWarningDisposal(bo, pageQuery);
    }

    @Override
    public TableDataInfo<ThirdPartyWarningDisposalVo> getMonitorWarningDisposalFromRemote(ThirdPartyWarningDisposalBo bo, PageQuery pageQuery) {
        return monitorWarningService.getMonitorWarningDisposalFromRemote(bo, pageQuery);
    }

    @Override
    public TableDataInfo<ThirdPartyWarningDisposalVo> getMonitorWarningDisposalFromRemote(
        ThirdPartyWarningDisposalBo bo,
        PageQuery pageQuery,
        LocalDateTime warningStartTime,
        LocalDateTime warningEndTime
    ) {
        return monitorWarningService.getMonitorWarningDisposalFromRemote(bo, pageQuery, warningStartTime, warningEndTime);
    }

    @Override
    public ThirdPartyDeviceCureVo queryDeviceCure(ThirdPartyDeviceCureBo bo) {
        return deviceCurveQueryService.queryDeviceCure(bo);
    }

    @Override
    public ThirdPartyWarningDataResp queryDeviceCureDirect(ThirdPartyDeviceCureDirectBo bo) {
        return deviceCurveQueryService.queryDeviceCureDirect(bo);
    }

    @Override
    public ThirdPartyDeviceCureVo queryDeviceCureFromRemote(ThirdPartyDeviceCureBo bo) {
        return deviceCurveQueryService.queryDeviceCureFromRemote(bo);
    }

    @Override
    public TableDataInfo<ThirdPartyMonitorDeviceVo> getMonitorDevicePage(ThirdPartyMonitorDeviceBo bo, PageQuery pageQuery) {
        return monitorDeviceService.getMonitorDevicePage(bo, pageQuery);
    }

    @Override
    public TableDataInfo<ThirdPartyMonitorDeviceVo> getMonitorDevicePageFromRemote(ThirdPartyMonitorDeviceBo bo, PageQuery pageQuery) {
        return monitorDeviceService.getMonitorDevicePageFromRemote(bo, pageQuery);
    }

    @Override
    public List<ThirdPartyMonitorDeviceTreeVo> getMonitorDeviceTree(ThirdPartyMonitorDeviceTreeBo bo) {
        return monitorDeviceService.getMonitorDeviceTree(bo);
    }

    @Override
    public List<MonitorDeviceRuntimeVo> listAllMonitorDevicesRuntime(String regionCode, String slopeUnitId) {
        return monitorDeviceService.listAllMonitorDevicesRuntime(regionCode, slopeUnitId);
    }

    @Override
    public List<MonitorDeviceRuntimeVo> listAllMonitorDevicesRuntimeFromRemote(String regionCode) {
        return monitorDeviceService.listAllMonitorDevicesRuntimeFromRemote(regionCode);
    }

    @Override
    public StatThirdWarningVo statMonitorWarning(StatThirdWarningBo bo) {
        return monitorWarningService.statMonitorWarning(bo);
    }

    @Override
    public List<LiveRainVo> getLiveRain(String date) {
        return weatherWarningService.getLiveRain(date);
    }

    @Override
    public DeviceRainfallDisplacementVo getDeviceRainfallAndDisplacement(String deviceId) {
        return monitorDeviceService.getDeviceRainfallAndDisplacement(deviceId);
    }

    @Override
    public Map<String, Object> checkThirdPartyConnectivity() {
        return splitSupport.checkThirdPartyConnectivity();
    }
}
