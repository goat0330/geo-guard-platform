/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.controller;

import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperatorType;
import org.dromara.common.web.core.BaseController;
import cn.edu.pku.whai.geological.disaster.data.domain.dto.YbssSydwDto;
import cn.edu.pku.whai.geological.disaster.data.domain.dto.YbssSyfwDto;
import cn.edu.pku.whai.geological.disaster.data.domain.dto.YbssSyrkDto;
import cn.edu.pku.whai.geological.disaster.data.service.IDataHouseService;
import cn.edu.pku.whai.geological.disaster.data.service.IDataOrganizationService;
import cn.edu.pku.whai.geological.disaster.data.service.IDataPersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据映射Controller
 * 用于接收一标三实数据并保存到本地数据库
 *
 * @author system
 * @date 2024
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/dizai/dataMapping")
public class DataMappingController extends BaseController {

    private final IDataPersonService dataPersonService;
    private final IDataHouseService dataHouseService;
    private final IDataOrganizationService dataOrganizationService;

    /**
     * 批量保存人员数据
     *
     * @param dataList 人员数据列表
     * @return 操作结果
     */
    @Log(title = "一标三实人员数据", businessType = BusinessType.INSERT, isSaveRequestData = false, operatorType = OperatorType.PLATFORM)
    @PostMapping("/savePerson")
    public R<Void> savePerson(@Valid @RequestBody List<YbssSyrkDto> dataList) {
        dataPersonService.saveBatch(dataList);
        return R.ok();
    }

    /**
     * 批量保存房间数据
     *
     * @param dataList 房间数据列表
     * @return 操作结果
     */
    @Log(title = "一标三实房屋数据", businessType = BusinessType.INSERT, isSaveRequestData = false, operatorType = OperatorType.PLATFORM)
    @PostMapping("/saveHouse")
    public R<Void> saveHouse(@Valid @RequestBody List<YbssSyfwDto> dataList) {
        dataHouseService.saveBatch(dataList);
        return R.ok();
    }

    /**
     * 批量保存组织机构数据
     *
     * @param dataList 组织机构数据列表
     * @return 操作结果
     */
    @Log(title = "一标三实组织机构数据", businessType = BusinessType.INSERT, isSaveRequestData = false, operatorType = OperatorType.PLATFORM)
    @PostMapping("/saveOrganization")
    public R<Void> saveOrganization(@Valid @RequestBody List<YbssSydwDto> dataList) {
        dataOrganizationService.saveBatch(dataList);
        return R.ok();
    }
}
