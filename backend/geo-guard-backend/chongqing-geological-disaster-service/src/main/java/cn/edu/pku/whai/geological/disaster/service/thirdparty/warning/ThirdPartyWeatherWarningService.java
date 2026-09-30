/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.RiskWarningRecordBo;
import cn.edu.pku.whai.geological.disaster.data.domain.req.warning.FileInfoReq;
import cn.edu.pku.whai.geological.disaster.data.domain.req.warning.RiskWarningRecordReq;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.warning.*;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.LiveRainVo;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.RiskWarningRecordVo;
import cn.edu.pku.whai.geological.disaster.data.props.WarningDataProps;
import cn.edu.pku.whai.geological.disaster.data.service.IThirdPartyWeatherWarningService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.kongweiguang.http.client.Req;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class ThirdPartyWeatherWarningService implements IThirdPartyWeatherWarningService {

    private final WarningDataProps warningDataProps;
    private final ThirdPartyWarningRemoteSupport remoteSupport;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public TableDataInfo<RiskWarningRecordVo> getWeatherWarning(RiskWarningRecordBo bo, PageQuery pageQuery) {
        int pageNum = pageQuery == null || pageQuery.getPageNum() == null ? 1 : pageQuery.getPageNum();
        int pageSize = pageQuery == null || pageQuery.getPageSize() == null ? 10 : pageQuery.getPageSize();

        RiskWarningRecordReq req = new RiskWarningRecordReq();
        req.setPageIndex(pageNum);
        req.setPageSize(pageSize);
        req.setStartTime(bo == null ? null : bo.getStartTime());
        req.setEndTime(bo == null ? null : bo.getEndTime());
        req.setState(bo == null ? null : bo.getState());

        RiskWarningRecordPageResp pageResp = getRiskWarningRecordPage(req);
        if (pageResp == null || pageResp.getRecords() == null || pageResp.getRecords().isEmpty()) {
            return TableDataInfo.build(new ArrayList<>());
        }

        TableDataInfo<RiskWarningRecordVo> result = TableDataInfo.build(BeanUtil.copyToList(pageResp.getRecords(), RiskWarningRecordVo.class));
        result.setTotal(pageResp.getTotal());
        return result;
    }

    @Override
    public UserInfoResp loginAuth() {
        return remoteSupport.loginAuth();
    }

    @Override
    public RiskWarningRecordPageResp getRiskWarningRecordPage(RiskWarningRecordReq req) {
        if (req == null) {
            return null;
        }
        remoteSupport.ensureWarningSsoToken();
        if (req.getPageIndex() == null) {
            throw new ServiceException("页码不能为空");
        }
        if (req.getPageSize() == null) {
            throw new ServiceException("每页记录数不能为空");
        }
        if (StrUtil.isBlank(req.getStartTime())) {
            throw new ServiceException("开始时间不能为空");
        }
        if (StrUtil.isBlank(req.getEndTime())) {
            throw new ServiceException("结束时间不能为空");
        }
        if (StrUtil.isBlank(req.getState())) {
            throw new ServiceException("预警数据状态不能为空");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("pageindex", req.getPageIndex());
        params.put("pagesize", req.getPageSize());
        params.put("yjrq1", req.getStartTime());
        params.put("yjrq2", req.getEndTime());
        params.put("state", req.getState());

        ThirdPartyWarningDataResp resp = Req.post(warningDataProps.getUrl())
                                            .path("/qxfxyj/api/qxfxyj/fxyj/pageFxjg")
                                            .header("sso", remoteSupport.getToken())
                                            .json(params)
                                            .timeout(Duration.ofMinutes(3))
                                            .ok()
                                            .obj(ThirdPartyWarningDataResp.class);
        if (resp == null || !resp.isSuccess() || resp.getData() == null) {
            return null;
        }
        return JSONUtil.toBean(JSONUtil.parseObj(resp.getData()), RiskWarningRecordPageResp.class);
    }

    @Override
    public RiskWarningRecordResp weatherWarningDetail(String id) {
        if (StrUtil.isBlank(id)) {
            throw new ServiceException("预警数据id不能为空");
        }
        remoteSupport.ensureWarningSsoToken();
        ThirdPartyWarningDataResp resp = Req.get(warningDataProps.getUrl())
                                            .path("/qxfxyj/api/qxfxyj/fxyj/getById")
                                            .header("sso", remoteSupport.getToken())
                                            .query("id", id)
                                            .timeout(Duration.ofMinutes(3))
                                            .ok()
                                            .obj(ThirdPartyWarningDataResp.class);
        if (resp == null || !resp.isSuccess() || resp.getData() == null) {
            throw new ServiceException("获取预警详情失败");
        }
        return JSONUtil.toBean(JSONUtil.parseObj(resp.getData()), RiskWarningRecordResp.class);
    }

    @Override
    public List<FileInfoResp> getFileInfo(FileInfoReq req) {
        if (req == null) {
            return null;
        }
        if (StrUtil.isBlank(req.getId())) {
            throw new ServiceException("预警数据id不能为空");
        }
        if (StrUtil.isBlank(req.getRefType())) {
            throw new ServiceException("文件类型不能为空");
        }

        remoteSupport.ensureWarningSsoToken();
        Map<String, Object> params = new HashMap<>();
        params.put("refid", req.getId());
        params.put("reftype", req.getRefType());
        ThirdPartyWarningDataResp resp = Req.post(warningDataProps.getUrl())
                                            .path("/qxfxyj/api/qxfxyj/file/list")
                                            .header("sso", remoteSupport.getToken())
                                            .json(params)
                                            .timeout(Duration.ofMinutes(3))
                                            .ok()
                                            .obj(ThirdPartyWarningDataResp.class);
        if (resp == null || !resp.isSuccess() || resp.getData() == null) {
            return new ArrayList<>();
        }
        return JSONUtil.toList(JSONUtil.parseArray(resp.getData()), FileInfoResp.class);
    }

    @Override
    public FileDataResp getAnalysisResult(String id) {
        remoteSupport.ensureWarningSsoToken();
        ResponseBody body = Req.post(warningDataProps.getUrl())
                               .path("/qxfxyj/api/qxfxyj/fxyj/expFxjg")
                               .header("sso", remoteSupport.getToken())
                               .json(Map.of("refid", id))
                               .timeout(Duration.ofMinutes(3))
                               .ok()
                               .body();
        return readFileBody(body);
    }

    @Override
    public FileDataResp getFileById(String fileId) {
        remoteSupport.ensureWarningSsoToken();
        ResponseBody body = Req.get(warningDataProps.getUrl())
                               .path("/qxfxyj/api/qxfxyj/file/view")
                               .header("sso", remoteSupport.getToken())
                               .query("fileid", fileId)
                               .timeout(Duration.ofMinutes(3))
                               .ok()
                               .body();
        return readFileBody(body);
    }

    @Override
    public List<LiveRainVo> getLiveRain(String date) {
        remoteSupport.ensureWarningSsoToken();
        LocalDateTime dateTime = LocalDateTime.parse(date, DATE_TIME_FORMATTER);
        String time = dateTime.withMinute(0).withSecond(0).format(DATE_TIME_FORMATTER);
        log.info("获取雨量数据: {}", time);
        try {
            ThirdPartyWarningDataResp resp = Req.post(warningDataProps.getUrl())
                                                .path("/qxfxyj/api/qxfxyj/rain/statistic/getLiveRain")
                                                .header("sso", remoteSupport.getToken())
                                                .json(Map.of("time", time))
                                                .timeout(Duration.ofMinutes(3))
                                                .ok()
                                                .obj(ThirdPartyWarningDataResp.class);
            if (resp == null || resp.getData() == null) {
                return buildDefaultLiveRainVos();
            }

            JSONObject dataObj = JSONUtil.parseObj(resp.getData());
            JSONArray features = dataObj.getJSONArray("features");
            if (features == null) {
                return buildDefaultLiveRainVos();
            }

            List<String> filters = Arrays.asList("恩施", "利川", "建始", "巴东", "宣恩", "鹤峰", "来凤", "来风", "咸丰");
            List<String> decides = Arrays.asList("恩施", "利川");
            List<LiveRainVo> result = new ArrayList<>();
            for (int i = 0; i < features.size(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject properties = feature.getJSONObject("properties");
                String name = properties.getStr("name");
                if (!filters.contains(name)) {
                    continue;
                }
                LiveRainVo vo = new LiveRainVo();
                JSONArray coordArray = feature.getJSONObject("geometry").getJSONArray("coordinates");
                List<Double> coordinates = new ArrayList<>();
                for (int j = 0; j < coordArray.size(); j++) {
                    coordinates.add(coordArray.getDouble(j));
                }
                vo.setCoordinates(coordinates);
                vo.setValue(properties.getDouble("001"));
                String regionName = "来风".equals(name) ? "来凤" : name;
                vo.setName(decides.contains(regionName) ? regionName + "市" : regionName + "县");
                result.add(vo);
            }
            return result.isEmpty() ? buildDefaultLiveRainVos() : result;
        } catch (Exception ex) {
            log.error("获取监测预警数据失败: {}", ex.getMessage());
            return buildDefaultLiveRainVos();
        }
    }

    private FileDataResp readFileBody(ResponseBody body) {
        try {
            MediaType mediaType = body.contentType();
            return new FileDataResp(body.bytes(), mediaType);
        } catch (IOException e) {
            throw new ServiceException("读取文件流失败 " + e.getMessage());
        } finally {
            if (!body.source().isOpen()) {
                body.close();
            }
        }
    }

    private List<LiveRainVo> buildDefaultLiveRainVos() {
        List<LiveRainVo> defaultList = new ArrayList<>();
        defaultList.add(buildLiveRainVo("恩施市", 109.466667, 30.283333));
        defaultList.add(buildLiveRainVo("利川市", 108.933334, 30.283333));
        defaultList.add(buildLiveRainVo("建始县", 109.716667, 30.6));
        defaultList.add(buildLiveRainVo("巴东县", 110.400002, 31.066668));
        defaultList.add(buildLiveRainVo("宣恩县", 109.48333, 30.0));
        defaultList.add(buildLiveRainVo("鹤峰县", 110.033333, 29.9));
        defaultList.add(buildLiveRainVo("来凤县", 109.416664, 29.516666));
        defaultList.add(buildLiveRainVo("咸丰县", 109.150002, 29.683332));
        return defaultList;
    }

    private LiveRainVo buildLiveRainVo(String name, Double lon, Double lat) {
        LiveRainVo vo = new LiveRainVo();
        vo.setName(name);
        vo.setCoordinates(Arrays.asList(lon, lat));
        vo.setValue(0.0);
        return vo;
    }
}
