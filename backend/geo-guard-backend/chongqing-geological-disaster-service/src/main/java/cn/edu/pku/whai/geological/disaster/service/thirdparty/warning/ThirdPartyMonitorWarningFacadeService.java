/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RiskWarningRecordBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.StatThirdWarningBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyWarningDisposalBo;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.MonitorWarningRecordResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.StatThirdWarningVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyWarningDisposalVo;
import cn.edu.pku.whai.geological.disaster.data.service.IThirdPartyMonitorWarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Primary
@RequiredArgsConstructor
public class ThirdPartyMonitorWarningFacadeService implements IThirdPartyMonitorWarningService {

    private final ThirdPartyWarningDataSplitSupport splitSupport;

    @Override
    public TableDataInfo<MonitorWarningRecordResp> getMonitorWarning(RiskWarningRecordBo bo, PageQuery pageQuery) {
        return splitSupport.getMonitorWarning(bo, pageQuery);
    }

    @Override
    public TableDataInfo<ThirdPartyWarningDisposalVo> getMonitorWarningDisposal(ThirdPartyWarningDisposalBo bo, PageQuery pageQuery) {
        return splitSupport.getMonitorWarningDisposal(bo, pageQuery);
    }

    @Override
    public TableDataInfo<ThirdPartyWarningDisposalVo> getMonitorWarningDisposalFromRemote(ThirdPartyWarningDisposalBo bo, PageQuery pageQuery) {
        return splitSupport.getMonitorWarningDisposalFromRemote(bo, pageQuery);
    }

    @Override
    public TableDataInfo<ThirdPartyWarningDisposalVo> getMonitorWarningDisposalFromRemote(
        ThirdPartyWarningDisposalBo bo,
        PageQuery pageQuery,
        LocalDateTime warningStartTime,
        LocalDateTime warningEndTime
    ) {
        return splitSupport.getMonitorWarningDisposalFromRemote(bo, pageQuery, warningStartTime, warningEndTime);
    }

    @Override
    public StatThirdWarningVo statMonitorWarning(StatThirdWarningBo bo) {
        return splitSupport.statMonitorWarning(bo);
    }
}
