/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RiskWarningRecordBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.StatThirdWarningBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyWarningDisposalBo;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.MonitorWarningRecordResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.StatThirdWarningVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyWarningDisposalVo;

public interface IThirdPartyMonitorWarningService {

    TableDataInfo<MonitorWarningRecordResp> getMonitorWarning(RiskWarningRecordBo bo, PageQuery pageQuery);

    TableDataInfo<ThirdPartyWarningDisposalVo> getMonitorWarningDisposal(ThirdPartyWarningDisposalBo bo, PageQuery pageQuery);

    TableDataInfo<ThirdPartyWarningDisposalVo> getMonitorWarningDisposalFromRemote(ThirdPartyWarningDisposalBo bo, PageQuery pageQuery);

    TableDataInfo<ThirdPartyWarningDisposalVo> getMonitorWarningDisposalFromRemote(
        ThirdPartyWarningDisposalBo bo,
        PageQuery pageQuery,
        java.time.LocalDateTime warningStartTime,
        java.time.LocalDateTime warningEndTime
    );

    StatThirdWarningVo statMonitorWarning(StatThirdWarningBo bo);
}
