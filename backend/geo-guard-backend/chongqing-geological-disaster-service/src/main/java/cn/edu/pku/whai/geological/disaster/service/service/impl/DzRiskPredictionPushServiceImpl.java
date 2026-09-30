/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzRiskPredictionPushBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskPredictionPush;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzRiskPredictionPushVo;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.RiskPredictionPushReportVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzMsgNoticeMapper;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskPredictionPushMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzRiskPredictionPushService;
import cn.edu.pku.whai.geological.disaster.service.sse.SseEmitterManager;
import cn.edu.pku.whai.geological.disaster.service.sse.domain.resp.SseResp;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 地灾风险预测推送Service业务层处理
 *
 * @author system
 * @date 2026-03-03
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DzRiskPredictionPushServiceImpl implements IDzRiskPredictionPushService {

    private final DzRiskPredictionPushMapper baseMapper;
    private final SseEmitterManager sseEmitterManager;
    private final DzMsgNoticeMapper dzMsgNoticeMapper;

    /**
     * 查询地灾风险预测推送
     *
     * @param id 主键
     * @return 地灾风险预测推送
     */
    @Override
    public DzRiskPredictionPushVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询地灾风险预测推送列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 地灾风险预测推送分页列表
     */
    @Override
    public TableDataInfo<DzRiskPredictionPushVo> queryPageList(DzRiskPredictionPushBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DzRiskPredictionPush> lqw = buildQueryWrapper(bo);
        Page<DzRiskPredictionPushVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的地灾风险预测推送列表
     *
     * @param bo 查询条件
     * @return 地灾风险预测推送列表
     */
    @Override
    public List<DzRiskPredictionPushVo> queryList(DzRiskPredictionPushBo bo) {
        LambdaQueryWrapper<DzRiskPredictionPush> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<DzRiskPredictionPush> buildQueryWrapper(DzRiskPredictionPushBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<DzRiskPredictionPush> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getPredictionBatchId() != null, DzRiskPredictionPush::getPredictionBatchId, bo.getPredictionBatchId());
        lqw.like(StrUtil.isNotEmpty(bo.getTitle()), DzRiskPredictionPush::getTitle, bo.getTitle());
        lqw.eq(bo.getType() != null, DzRiskPredictionPush::getType, bo.getType());
        lqw.eq(bo.getStatus() != null, DzRiskPredictionPush::getStatus, bo.getStatus());
        lqw.eq(bo.getDocId() != null, DzRiskPredictionPush::getDocId, bo.getDocId());

        if (params.get("beginTime") != null && params.get("endTime") != null) {
            lqw.between(DzRiskPredictionPush::getCreateDate, params.get("beginTime"), params.get("endTime"));
        }

        return lqw;
    }

    /**
     * 新增地灾风险预测推送
     *
     * @param bo 地灾风险预测推送
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(DzRiskPredictionPushBo bo) {
        DzRiskPredictionPush add = MapstructUtils.convert(bo, DzRiskPredictionPush.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改地灾风险预测推送
     *
     * @param bo 地灾风险预测推送
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(DzRiskPredictionPushBo bo) {
        DzRiskPredictionPush update = MapstructUtils.convert(bo, DzRiskPredictionPush.class);
        if (bo == null) {
            throw new ServiceException("风险预测推送不能为空");
        }
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(DzRiskPredictionPush entity) {
        if (entity == null) {
            throw new ServiceException("风险预测推送不能为空");
        }
        if (entity.getPredictionBatchId() == null) {
            throw new ServiceException("预测批次不能为空");
        }
        if (StrUtil.isBlank(entity.getTitle())) {
            throw new ServiceException("推送标题不能为空");
        }
        if (StrUtil.isBlank(entity.getContent())) {
            throw new ServiceException("推送内容不能为空");
        }
        if (entity.getType() == null || entity.getType() < 1 || entity.getType() > 5) {
            throw new ServiceException("推送类型非法");
        }
        if (entity.getStatus() != null && !List.of(0, 1).contains(entity.getStatus())) {
            throw new ServiceException("推送状态非法");
        }
    }

    /**
     * 校验并批量删除地灾风险预测推送信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            List<DzRiskPredictionPush> pushes = baseMapper.selectByIds(ids);
            if (pushes.size() != ids.size()) {
                throw new ServiceException("存在推送记录不存在，无法删除");
            }
            if (pushes.stream().anyMatch(item -> Integer.valueOf(1).equals(item.getStatus()))) {
                throw new ServiceException("已推送记录不允许删除");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public RiskPredictionPushReportVo pushReport(Long id) {
        DzRiskPredictionPush dzRiskPredictionPush = baseMapper.selectById(id);
        if (dzRiskPredictionPush == null) {
            throw new ServiceException("数据不存在");
        }
        if (Integer.valueOf(1).equals(dzRiskPredictionPush.getStatus())) {
            throw new ServiceException("该预测推送已发布，请勿重复推送");
        }
        if (sseEmitterManager.getUserCount() <= 0) {
            throw new ServiceException("当前无在线接收对象，推送已取消");
        }

        SseResp.SseRespBuilder msg = SseResp.builder().type("prediction-push-report").data(dzRiskPredictionPush);
        // todo 推送到四级六位责任体系相关负责人
        sseEmitterManager.sendMessage(msg);
        dzRiskPredictionPush.setStatus(1);
        dzRiskPredictionPush.setPerson(StrUtil.join(",", sseEmitterManager.getAllEmitters().keySet()));
        dzRiskPredictionPush.setPersonCount(sseEmitterManager.getUserCount());
        baseMapper.updateById(dzRiskPredictionPush);
        RiskPredictionPushReportVo vo = new RiskPredictionPushReportVo();
        vo.setPersonCount(dzRiskPredictionPush.getPersonCount());
        return vo;
    }
}
