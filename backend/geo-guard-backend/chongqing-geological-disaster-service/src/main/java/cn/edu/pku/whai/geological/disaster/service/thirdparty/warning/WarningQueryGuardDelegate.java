package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import org.dromara.common.core.exception.ServiceException;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RiskWarningRecordBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyWarningDisposalBo;
import cn.hutool.core.util.StrUtil;

/**
 * 三方预警数据拆分处理服务（从 ThirdPartyWarningDataServiceImpl 提取为独立文件）。
 *
 * @author kongweiguang
 */
class WarningQueryGuardDelegate {
    private final ThirdPartyWarningDataSplitSupport support;

    WarningQueryGuardDelegate(ThirdPartyWarningDataSplitSupport support) {
        this.support = support;
    }

    void validateParams(RiskWarningRecordBo bo) {
        if (bo == null) {
            throw new ServiceException("查询条件不能为空");
        }
        if (StrUtil.isBlank(bo.getStartTime())) {
            throw new ServiceException("开始时间不能为空");
        }
        if (StrUtil.isBlank(bo.getEndTime())) {
            throw new ServiceException("结束时间不能为空");
        }
    }

    /**
     * 校验预警处置查询参数
     */
    void validateMonitorWarningDisposalParams(ThirdPartyWarningDisposalBo bo) {
        if (StrUtil.isBlank(bo.getRegionCode()) && StrUtil.isBlank(bo.getMonitorPointName())) {
            throw new ServiceException("行政区划或监测点名称不能为空");
        }
        support.getWarningDisposalDelegate().validateDateParam(bo.getWarningPublishTime(), "预警发布时间");
        support.getWarningDisposalDelegate().validateDateParam(bo.getWarningDisposalTime(), "预警处置时间");
    }



}
