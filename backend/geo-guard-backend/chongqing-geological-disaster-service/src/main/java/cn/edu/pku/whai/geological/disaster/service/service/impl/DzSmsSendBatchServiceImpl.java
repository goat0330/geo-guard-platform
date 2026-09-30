/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzSmsSendBatchBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSmsSendBatch;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzSmsSendBatchVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzSmsSendBatchMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzSmsSendBatchService;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 短信发送批次汇总服务实现。
 */
@Service
@RequiredArgsConstructor
public class DzSmsSendBatchServiceImpl implements IDzSmsSendBatchService {

    private final DzSmsSendBatchMapper baseMapper;

    @Override
    public Boolean insertByBo(DzSmsSendBatchBo bo) {
        DzSmsSendBatch add = MapstructUtils.convert(bo, DzSmsSendBatch.class);
        validEntityBeforeSave(add);
        if (add.getCreateDate() == null) {
            add.setCreateDate(new Date());
        }
        if (add.getUpdateDate() == null) {
            add.setUpdateDate(new Date());
        }
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public boolean existsRecentByPhoneAndContent(String phone, String content, Date since) {
        if (StringUtils.isBlank(phone) || StringUtils.isBlank(content) || since == null) {
            return false;
        }
        return Boolean.TRUE.equals(baseMapper.existsRecentByPhoneAndContent(phone.trim(), content, since));
    }

    @Override
    public List<DzSmsSendBatchVo> queryByBiz(Integer bizType, Long bizId) {
        if (bizType == null || bizId == null) {
            return List.of();
        }
        return baseMapper.selectVoList(
            Wrappers.<DzSmsSendBatch>lambdaQuery()
                .eq(DzSmsSendBatch::getBizType, bizType)
                .eq(DzSmsSendBatch::getBizId, bizId)
                .orderByDesc(DzSmsSendBatch::getRequestTime, DzSmsSendBatch::getUpdateDate, DzSmsSendBatch::getId)
        );
    }

    private void validEntityBeforeSave(DzSmsSendBatch entity) {
        if (entity == null) {
            throw new ServiceException("短信发送批次不能为空");
        }
        Assert.notBlank(entity.getBatchId(), "批次ID不能为空");
        Assert.notNull(entity.getBizType(), "业务类型不能为空");
        Assert.notNull(entity.getBizId(), "业务对象ID不能为空");
        Assert.notBlank(entity.getSmsType(), "短信类型不能为空");
        Assert.notBlank(entity.getTargetType(), "发送对象类型不能为空");
        Assert.notBlank(entity.getSendStatus(), "发送状态不能为空");
        Assert.notNull(entity.getSuccessCount(), "发送成功数不能为空");
        Assert.notNull(entity.getFailCount(), "发送失败数不能为空");
        Assert.notNull(entity.getTotalCount(), "发送总数不能为空");
    }
}
