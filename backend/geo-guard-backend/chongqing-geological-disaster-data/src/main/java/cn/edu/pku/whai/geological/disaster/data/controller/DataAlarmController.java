/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.data.domain.bo.DataAlarmBo;
import cn.edu.pku.whai.geological.disaster.data.domain.req.DataAlarmPendingStatusBatchUpdateReq;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.DataAlarmPendingGroupResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.DataAlarmPendingStatusBatchUpdateResp;
import cn.edu.pku.whai.geological.disaster.data.domain.resp.WeatherAlarmStatsResp;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataAlarmVo;
import cn.edu.pku.whai.geological.disaster.data.service.IDataAlarmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/dataAlarm")
public class DataAlarmController extends BaseController {

    private final IDataAlarmService dataAlarmService;


    /**
     * 获取气象预警统计数据：id为空取今天，id有值按指定告警统计
     */
    @GetMapping("/Stats")
    public R<WeatherAlarmStatsResp> stats(@RequestParam(required = false) String id) {
        if (StringUtils.isBlank(id)) {
            return R.ok(dataAlarmService.todayStats());
        }
        long idLong;
        try {
            idLong = Long.parseLong(id.trim());
        } catch (NumberFormatException e) {
            throw new ServiceException("id格式错误，必须为数字");
        }
        return R.ok(dataAlarmService.getWeatherAlarmStats(idLong));
    }


    /**
     * 获取所有气象预警数据
     */
    @GetMapping("/list")
    public R<TableDataInfo<DataAlarmVo>> list(DataAlarmBo bo, PageQuery pageQuery) {
        return R.ok(dataAlarmService.queryPageList(bo, pageQuery));
    }

    /**
     * 按指定时间匹配气象预警列表
     */
    @GetMapping("/matchList")
    public R<List<DataAlarmVo>> matchList(@RequestParam("data")
                                          @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date data) {
        if (data == null) {
            throw new ServiceException("data不能为空");
        }
        return R.ok(dataAlarmService.listMatchByData(data));
    }

    /**
     * 查询未触发预警分组列表
     */
    @GetMapping("/pending/groupList")
    public R<List<DataAlarmPendingGroupResp>> pendingGroupList() {
        return R.ok(dataAlarmService.queryPendingGroupList());
    }

    /**
     * 根据 session_id 查询关联的消息ID列表
     */
    @GetMapping("/codesBySessionId")
    public R<List<String>> listCodesBySessionId(@RequestParam("sessionId") String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            throw new ServiceException("session_id不能为空");
        }
        return R.ok(dataAlarmService.listCodesBySessionId(sessionId));
    }

    /**
     * 人工补录警报数据
     */
    @Log(title = "气象预警", businessType = BusinessType.INSERT, operatorType = OperatorType.PLATFORM)
    @PostMapping()
    public R<Long> manualCreate(@RequestBody DataAlarmBo bo) {
        return R.ok(dataAlarmService.insertManualByBo(bo));
    }

    /**
     * 修改人工补录警报数据
     */
    @Log(title = "气象预警", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PutMapping()
    public R<Long> manualUpdate(@RequestBody DataAlarmBo bo) {
        return R.ok(dataAlarmService.updateManualByBo(bo));
    }

    /**
     * 批量修改待触发状态
     */
    @Log(title = "气象预警", businessType = BusinessType.UPDATE, operatorType = OperatorType.PLATFORM)
    @PutMapping("/pendingStatus/batch")
    public R<DataAlarmPendingStatusBatchUpdateResp> batchUpdatePendingStatus(@Valid @RequestBody DataAlarmPendingStatusBatchUpdateReq req) {
        return R.ok(dataAlarmService.batchUpdatePendingStatus(req.getIds(), req.getPendingStatus()));
    }
}
