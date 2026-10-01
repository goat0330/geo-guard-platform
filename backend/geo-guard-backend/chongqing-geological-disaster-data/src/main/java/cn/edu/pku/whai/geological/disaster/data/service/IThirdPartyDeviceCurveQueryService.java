/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyDeviceCureBo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.ThirdPartyDeviceCureDirectBo;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.ThirdPartyWarningDataResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.ThirdPartyDeviceCureVo;

public interface IThirdPartyDeviceCurveQueryService {

    ThirdPartyDeviceCureVo queryDeviceCure(ThirdPartyDeviceCureBo bo);

    ThirdPartyWarningDataResp queryDeviceCureDirect(ThirdPartyDeviceCureDirectBo bo);

    ThirdPartyDeviceCureVo queryDeviceCureFromRemote(ThirdPartyDeviceCureBo bo);
}
