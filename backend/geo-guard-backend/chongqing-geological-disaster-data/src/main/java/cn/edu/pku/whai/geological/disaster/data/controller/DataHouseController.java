/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.data.domain.req.DataHouseWktReq;
import cn.edu.pku.whai.geological.disaster.data.domain.dto.YbssSyfwDto;
import cn.edu.pku.whai.geological.disaster.data.domain.vo.DataHouseVo;
import cn.edu.pku.whai.geological.disaster.data.service.IDataHouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 房屋数据接收Controller
 *
 * @author system
 * @date 2026-05-14
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/dataHouse")
public class DataHouseController extends BaseController {

    private final IDataHouseService dataHouseService;

    /**
     * 批量保存房屋数据
     *
     * @param dataList 房屋数据列表
     * @return 操作结果
     */
    @Log(title = "一标三实房屋数据", businessType = BusinessType.INSERT, isSaveRequestData = false, operatorType = OperatorType.PLATFORM)
    @PostMapping("/saveBatch")
    public R<Void> saveBatch(@Valid @RequestBody List<YbssSyfwDto> dataList) {
        dataHouseService.saveBatch(dataList);
        return R.ok();
    }

    /**
     * 根据范围WKT查询房屋列表
     *
     * @param req 范围WKT
     * @return 房屋列表
     */
    @PostMapping("/queryByWkt")
    public R<List<DataHouseVo>> queryByWkt(@Valid @RequestBody DataHouseWktReq req) {
        return R.ok(dataHouseService.queryByWkt(req.getWkt()));
    }
}
