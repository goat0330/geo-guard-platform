/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanTypeEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DefRespPlanBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzTaskHandle;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzTaskHandleSceneRecordVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskHandleMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzSingleDefProgressService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleSceneRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
class DzDefRespSingleDelegate {

    private static final String DEF_RESP_CODE_MARKER_SINGLE = DzDefRespPlanServiceImpl.DEF_RESP_CODE_MARKER_SINGLE;
    private static final String KEY_TASKS_NUMBER = DzDefRespPlanServiceImpl.KEY_TASKS_NUMBER;

    private final DzDefRespPlanServiceImpl service;
    private final DzTaskHandleMapper dzTaskHandleMapper;
    private final IDzTaskHandleSceneRecordService dzTaskHandleSceneRecordService;
    private final IDzSingleDefProgressService dzSingleDefProgressService;

    DzDefRespSingleDelegate(DzDefRespPlanServiceImpl service,
                            DzTaskHandleMapper dzTaskHandleMapper,
                            IDzTaskHandleSceneRecordService dzTaskHandleSceneRecordService,
                            IDzSingleDefProgressService dzSingleDefProgressService) {
        this.service = service;
        this.dzTaskHandleMapper = dzTaskHandleMapper;
        this.dzTaskHandleSceneRecordService = dzTaskHandleSceneRecordService;
        this.dzSingleDefProgressService = dzSingleDefProgressService;
    }

    private String buildDefRespPlanCode(String marker) { return service.buildDefRespPlanCode(marker); }

    private boolean insertByBo(DefRespPlanBo bo) { return service.insertByBo(bo, true); }

    private DefRespPlan getDefRespPlanById(Long defId) { return service.getDefRespPlanById(defId); }
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startSingle(Long handleId) {
        if (handleId == null) {
            throw new ServiceException("handleId不能为空");
        }

        DzTaskHandle dzTaskHandle = dzTaskHandleMapper.selectById(handleId);
        if (dzTaskHandle == null) {
            throw new ServiceException("未查询到对应的灾害处置记录, handleId=" + handleId);
        }
        Date date = new Date();
        DefRespPlanBo bo = new DefRespPlanBo();
        bo.setHandleId(handleId);
        bo.setType(DefRespPlanTypeEnum.SINGLE.getCode());
        bo.setStatus(DefRespPlanStatusEnum.STARTED.getCode());
        bo.setCode(buildDefRespPlanCode(DEF_RESP_CODE_MARKER_SINGLE));
        bo.setName(dzTaskHandle.getProvince() + dzTaskHandle.getCity() + dzTaskHandle.getStreet() + "_单点防御响应方案_" + handleId);
        bo.setCounty(dzTaskHandle.getCounty());
        bo.setStreets(dzTaskHandle.getStreet());
        DzTaskHandleSceneRecordVo dzTaskHandleSceneRecordVo = dzTaskHandleSceneRecordService.getByHandleId(handleId);
        if (dzTaskHandleSceneRecordVo == null) {
            throw new ServiceException("未找到handleId对应的现场记录");
        }
        bo.setCenter(dzTaskHandleSceneRecordVo.getDisasterCoordinates());
        bo.setTriggerCondition(dzTaskHandleSceneRecordVo.getDisasterName());
        bo.setResponsibilityUnit(dzTaskHandle.getStreet() + "人民政府");
        bo.setResponsiblePerson(dzTaskHandle.getResponsiblePerson());
        bo.setResponsiblePersonPhone(dzTaskHandle.getResponsiblePersonPhone());
        bo.setLevel(dzTaskHandle.getEventLevel());
        bo.setCurrentRoundNo(1);
        bo.setCreateDate(date);
        if (!insertByBo(bo)) {
            log.error("新增防御响应方案失败");
            throw new ServiceException("新增防御响应方案失败");
        }
        dzTaskHandle.setRespStatus(dzTaskHandle.getEventLevel());
        dzTaskHandleMapper.updateById(dzTaskHandle);
        Long defRespPlanId = bo.getId();
        DefRespPlan singlePlan = getDefRespPlanById(defRespPlanId);
        dzSingleDefProgressService.recordStarted(singlePlan, dzTaskHandle, LoginHelper.getUserId(), date);
        service.recordSingleDefRespStartProcess(dzTaskHandle, singlePlan, date);
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_TASKS_NUMBER, 0);
        return params;
    }

}
