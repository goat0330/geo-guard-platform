/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import cn.edu.pku.whai.geological.disaster.service.domain.bo.DzSmsSendStatBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSmsSendBatch;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzSmsSendStat;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.DzSmsSendStatVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzSmsSendStatMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzSmsSendStatService;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 短信发送单条记录服务实现。
 */
@Service
@RequiredArgsConstructor
public class DzSmsSendStatServiceImpl implements IDzSmsSendStatService {

    private final DzSmsSendStatMapper baseMapper;

    @Override
    public DzSmsSendStatVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<DzSmsSendStatVo> queryPageList(DzSmsSendStatBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DzSmsSendStat> lqw = buildQueryWrapper(bo);
        Page<DzSmsSendStatVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<DzSmsSendStatVo> queryList(DzSmsSendStatBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    public boolean existsRecentByPhoneAndContent(String phone, String content, Date since) {
        if (StringUtils.isBlank(phone) || StringUtils.isBlank(content) || since == null) {
            return false;
        }
        return baseMapper.exists(Wrappers.<DzSmsSendStat>lambdaQuery()
            .eq(DzSmsSendStat::getReceiverPhone, phone.trim())
            .eq(DzSmsSendStat::getSmsContent, content)
            .eq(DzSmsSendStat::getSendStatus, DzSmsSendBatch.SEND_STATUS_SUCCESS)
            .ge(DzSmsSendStat::getSendTime, since));
    }

    @Override
    public Boolean insertByBo(DzSmsSendStatBo bo) {
        DzSmsSendStat add = MapstructUtils.convert(bo, DzSmsSendStat.class);
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
    public Boolean updateByBo(DzSmsSendStatBo bo) {
        DzSmsSendStat update = MapstructUtils.convert(bo, DzSmsSendStat.class);
        validEntityBeforeSave(update);
        update.setUpdateDate(new Date());
        return baseMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        if (isValid) {
            List<DzSmsSendStat> list = baseMapper.selectByIds(ids);
            if (list.size() != ids.size()) {
                throw new ServiceException("存在短信发送统计数据不存在，无法删除");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    private LambdaQueryWrapper<DzSmsSendStat> buildQueryWrapper(DzSmsSendStatBo bo) {
        LambdaQueryWrapper<DzSmsSendStat> lqw = Wrappers.lambdaQuery();
        if (bo == null) {
            lqw.orderByDesc(DzSmsSendStat::getUpdateDate, DzSmsSendStat::getId);
            return lqw;
        }
        lqw.eq(bo.getId() != null, DzSmsSendStat::getId, bo.getId());
        lqw.like(StringUtils.isNotBlank(bo.getBatchId()), DzSmsSendStat::getBatchId, bo.getBatchId());
        lqw.eq(StringUtils.isNotBlank(bo.getSmsType()), DzSmsSendStat::getSmsType, bo.getSmsType());
        lqw.eq(StringUtils.isNotBlank(bo.getSceneCode()), DzSmsSendStat::getSceneCode, bo.getSceneCode());
        lqw.eq(bo.getTaskSourceType() != null, DzSmsSendStat::getTaskSourceType, bo.getTaskSourceType());
        lqw.eq(bo.getBizType() != null, DzSmsSendStat::getBizType, bo.getBizType());
        lqw.eq(bo.getBizId() != null, DzSmsSendStat::getBizId, bo.getBizId());
        lqw.like(StringUtils.isNotBlank(bo.getReceiverName()), DzSmsSendStat::getReceiverName, bo.getReceiverName());
        lqw.eq(StringUtils.isNotBlank(bo.getReceiverPhone()), DzSmsSendStat::getReceiverPhone, bo.getReceiverPhone());
        lqw.like(StringUtils.isNotBlank(bo.getSmsContent()), DzSmsSendStat::getSmsContent, bo.getSmsContent());
        lqw.eq(StringUtils.isNotBlank(bo.getSendStatus()), DzSmsSendStat::getSendStatus, bo.getSendStatus());
        lqw.like(StringUtils.isNotBlank(bo.getErrorMsg()), DzSmsSendStat::getErrorMsg, bo.getErrorMsg());
        lqw.eq(bo.getSendTime() != null, DzSmsSendStat::getSendTime, bo.getSendTime());
        lqw.eq(bo.getCreateDate() != null, DzSmsSendStat::getCreateDate, bo.getCreateDate());
        lqw.eq(bo.getUpdateDate() != null, DzSmsSendStat::getUpdateDate, bo.getUpdateDate());
        lqw.orderByDesc(DzSmsSendStat::getSendTime, DzSmsSendStat::getUpdateDate, DzSmsSendStat::getId);
        return lqw;
    }

    private void validEntityBeforeSave(DzSmsSendStat entity) {
        Assert.notBlank(entity.getBatchId(), "批次ID不能为空");
        Assert.notNull(entity.getBizType(), "业务类型不能为空");
        Assert.notNull(entity.getBizId(), "业务对象ID不能为空");
        Assert.notBlank(entity.getSmsType(), "短信类型不能为空");
        Assert.notBlank(entity.getReceiverPhone(), "接收人手机号不能为空");
        Assert.notBlank(entity.getSendStatus(), "发送状态不能为空");
    }
}
