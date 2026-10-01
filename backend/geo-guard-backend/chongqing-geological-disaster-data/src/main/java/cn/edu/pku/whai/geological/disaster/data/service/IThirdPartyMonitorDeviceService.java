/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyMonitorDeviceBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyMonitorDeviceTreeBo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DeviceRainfallDisplacementVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.MonitorDeviceRuntimeVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyMonitorDeviceTreeVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyMonitorDeviceVo;

import java.util.List;

public interface IThirdPartyMonitorDeviceService {

    TableDataInfo<ThirdPartyMonitorDeviceVo> getMonitorDevicePage(ThirdPartyMonitorDeviceBo bo, PageQuery pageQuery);

    TableDataInfo<ThirdPartyMonitorDeviceVo> getMonitorDevicePageFromRemote(ThirdPartyMonitorDeviceBo bo, PageQuery pageQuery);

    List<ThirdPartyMonitorDeviceTreeVo> getMonitorDeviceTree(ThirdPartyMonitorDeviceTreeBo bo);

    List<MonitorDeviceRuntimeVo> listAllMonitorDevicesRuntime(String regionCode, String slopeUnitId);

    List<MonitorDeviceRuntimeVo> listAllMonitorDevicesRuntimeFromRemote(String regionCode);

    DeviceRainfallDisplacementVo getDeviceRainfallAndDisplacement(String deviceId);
}
