/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RiskWarningRecordBo;
import cn.edu.pku.whai.geological.disaster.data.domain.req.warning.FileInfoReq;
import cn.edu.pku.whai.geological.disaster.data.domain.req.warning.RiskWarningRecordReq;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.FileDataResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.FileInfoResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.RiskWarningRecordPageResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.RiskWarningRecordResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.UserInfoResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.LiveRainVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RiskWarningRecordVo;

import java.util.List;

public interface IThirdPartyWeatherWarningService {

    TableDataInfo<RiskWarningRecordVo> getWeatherWarning(RiskWarningRecordBo bo, PageQuery pageQuery);

    UserInfoResp loginAuth();

    RiskWarningRecordPageResp getRiskWarningRecordPage(RiskWarningRecordReq req);

    RiskWarningRecordResp weatherWarningDetail(String id);

    List<FileInfoResp> getFileInfo(FileInfoReq req);

    FileDataResp getAnalysisResult(String id);

    FileDataResp getFileById(String fileId);

    List<LiveRainVo> getLiveRain(String date);
}
