/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyMonitorDeviceBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyMonitorDeviceTreeBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DeviceRainfallDisplacementVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorDeviceRuntimeVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyMonitorDeviceTreeVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyMonitorDeviceVo;
import cn.edu.pku.whai.geological.disaster.data.service.IThirdPartyMonitorDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
@RequiredArgsConstructor
public class ThirdPartyMonitorDeviceFacadeService implements IThirdPartyMonitorDeviceService {

    private final ThirdPartyWarningDataSplitSupport splitSupport;

    @Override
    public TableDataInfo<ThirdPartyMonitorDeviceVo> getMonitorDevicePage(ThirdPartyMonitorDeviceBo bo, PageQuery pageQuery) {
        return splitSupport.getMonitorDevicePage(bo, pageQuery);
    }

    @Override
    public TableDataInfo<ThirdPartyMonitorDeviceVo> getMonitorDevicePageFromRemote(ThirdPartyMonitorDeviceBo bo, PageQuery pageQuery) {
        return splitSupport.getMonitorDevicePageFromRemote(bo, pageQuery);
    }

    @Override
    public List<ThirdPartyMonitorDeviceTreeVo> getMonitorDeviceTree(ThirdPartyMonitorDeviceTreeBo bo) {
        return splitSupport.getMonitorDeviceTree(bo);
    }

    @Override
    public List<MonitorDeviceRuntimeVo> listAllMonitorDevicesRuntime(String regionCode, String slopeUnitId) {
        return splitSupport.listAllMonitorDevicesRuntime(regionCode, slopeUnitId);
    }

    @Override
    public List<MonitorDeviceRuntimeVo> listAllMonitorDevicesRuntimeFromRemote(String regionCode) {
        return splitSupport.listAllMonitorDevicesRuntimeFromRemote(regionCode);
    }

    @Override
    public DeviceRainfallDisplacementVo getDeviceRainfallAndDisplacement(String deviceId) {
        return splitSupport.getDeviceRainfallAndDisplacement(deviceId);
    }
}
