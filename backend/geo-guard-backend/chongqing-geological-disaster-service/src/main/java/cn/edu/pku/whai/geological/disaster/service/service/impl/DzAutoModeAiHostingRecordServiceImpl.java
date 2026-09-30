/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzAutoModeAiHostingRecord;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.AiHostingRecordSessionVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzAutoModeAiHostingRecordMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutoModeAiHostingRecordService;
import cn.edu.pku.whai.geological.disaster.service.utils.CurrentRoleUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 自动模式AI托管运行记录服务实现。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DzAutoModeAiHostingRecordServiceImpl implements IDzAutoModeAiHostingRecordService {

    private static final String RECORD_NAME_DATE_PATTERN = "yyyyMMdd";
    private static final String RECORD_NAME_SUFFIX = "AI托管记录";

    private final DzAutoModeAiHostingRecordMapper baseMapper;

    @Override
    public AiHostingRecordSessionVo createOnOpen(Date openedAt) {
        Date safeOpenedAt = openedAt == null ? new Date() : openedAt;
        Date now = new Date();
        OperatorSnapshot operator = currentOperator();
        DzAutoModeAiHostingRecord record = new DzAutoModeAiHostingRecord();
        record.setId(IdUtil.getSnowflakeNextId());
        record.setRecordName(nextRecordName(safeOpenedAt));
        record.setOpenedAt(safeOpenedAt);
        record.setDurationMinutes(0);
        record.setDurationText(formatDurationText(0L));
        record.setOpenUserId(operator.userId());
        record.setOpenUserName(operator.userName());
        record.setOpenUserRole(operator.userRole());
        record.setCreateDate(now);
        record.setUpdateDate(now);
        baseMapper.insert(record);
        return toVo(record);
    }

    @Override
    public void closeLatest(Date closedAt) {
        DzAutoModeAiHostingRecord latest = baseMapper.selectOne(
            Wrappers.<DzAutoModeAiHostingRecord>lambdaQuery()
                .isNull(DzAutoModeAiHostingRecord::getClosedAt)
                .orderByDesc(DzAutoModeAiHostingRecord::getOpenedAt, DzAutoModeAiHostingRecord::getId)
                .last("limit 1")
        );
        if (latest == null) {
            log.warn("自动模式关闭时未找到运行中的AI托管记录");
            return;
        }
        Date safeClosedAt = closedAt == null ? new Date() : closedAt;
        OperatorSnapshot operator = currentOperator();
        int durationMinutes = durationMinutes(latest.getOpenedAt(), safeClosedAt);
        long durationSeconds = durationSeconds(latest.getOpenedAt(), safeClosedAt);
        DzAutoModeAiHostingRecord update = new DzAutoModeAiHostingRecord();
        update.setId(latest.getId());
        update.setClosedAt(safeClosedAt);
        update.setDurationMinutes(durationMinutes);
        update.setDurationText(formatDurationText(durationSeconds));
        update.setCloseUserId(operator.userId());
        update.setCloseUserName(operator.userName());
        update.setCloseUserRole(operator.userRole());
        update.setUpdateDate(new Date());
        baseMapper.updateById(update);
    }

    @Override
    public AiHostingRecordSessionVo queryById(Long id) {
        if (id == null) {
            throw new ServiceException("AI托管记录ID不能为空");
        }
        AiHostingRecordSessionVo vo = fillRuntimeFields(baseMapper.selectVoById(id));
        if (vo == null) {
            throw new ServiceException("AI托管记录不存在");
        }
        return vo;
    }

    @Override
    public AiHostingRecordSessionVo queryLatest() {
        return fillRuntimeFields(baseMapper.selectVoOne(
            Wrappers.<DzAutoModeAiHostingRecord>lambdaQuery()
                .orderByDesc(DzAutoModeAiHostingRecord::getOpenedAt, DzAutoModeAiHostingRecord::getId)
                .last("limit 1")
        ));
    }

    @Override
    public TableDataInfo<AiHostingRecordSessionVo> queryPageList(PageQuery pageQuery) {
        Page<AiHostingRecordSessionVo> page = baseMapper.selectVoPage(
            pageQuery.build(),
            Wrappers.<DzAutoModeAiHostingRecord>lambdaQuery()
                .isNotNull(DzAutoModeAiHostingRecord::getClosedAt)
                .orderByDesc(DzAutoModeAiHostingRecord::getOpenedAt, DzAutoModeAiHostingRecord::getId)
        );
        if (page.getRecords() != null) {
            page.getRecords().forEach(this::fillRuntimeFields);
        }
        return TableDataInfo.build(page);
    }

    static int durationMinutes(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            return 0;
        }
        long minutes = Duration.between(startTime.toInstant(), endTime.toInstant()).toMinutes();
        return (int) Math.max(0L, minutes);
    }

    static String formatDurationText(Integer minutes) {
        long safeMinutes = Math.max(0, Objects.requireNonNullElse(minutes, 0));
        return formatDurationText(safeMinutes * 60L);
    }

    static long durationSeconds(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            return 0L;
        }
        return Math.max(0L, Duration.between(startTime.toInstant(), endTime.toInstant()).toSeconds());
    }

    static String formatDurationText(long seconds) {
        return DzAiHostingServiceImpl.formatDurationText(seconds);
    }

    private String nextRecordName(Date openedAt) {
        Date dayStart = DateUtil.beginOfDay(openedAt);
        Date nextDayStart = DateUtil.offsetDay(dayStart, 1);
        long count = baseMapper.selectCount(
            Wrappers.<DzAutoModeAiHostingRecord>lambdaQuery()
                .ge(DzAutoModeAiHostingRecord::getOpenedAt, dayStart)
                .lt(DzAutoModeAiHostingRecord::getOpenedAt, nextDayStart)
        );
        return DateUtil.format(openedAt, RECORD_NAME_DATE_PATTERN)
            + "-"
            + String.format("%03d", count + 1)
            + RECORD_NAME_SUFFIX;
    }

    private OperatorSnapshot currentOperator() {
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            if (loginUser == null) {
                return OperatorSnapshot.empty();
            }
            String userName = StringUtils.defaultIfBlank(loginUser.getNickname(), loginUser.getUsername());
            String userRole = CurrentRoleUtil.requireCurrentRoles().stream()
                .map(role -> role == null ? null : role.getRoleName())
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining("，"));
            return new OperatorSnapshot(
                loginUser.getUserId(),
                userName,
                StringUtils.blankToDefault(userRole, null)
            );
        } catch (Exception e) {
            log.warn("解析当前AI托管操作人信息失败，将以空操作人写入记录", e);
            return OperatorSnapshot.empty();
        }
    }

    private AiHostingRecordSessionVo fillRuntimeFields(AiHostingRecordSessionVo vo) {
        if (vo == null) {
            return null;
        }
        vo.setRunning(vo.getClosedAt() == null);
        if (vo.getDurationMinutes() == null) {
            vo.setDurationMinutes(durationMinutes(vo.getOpenedAt(), firstNonNull(vo.getClosedAt(), new Date())));
        }
        vo.setDurationText(formatDurationText(resolveDurationSeconds(vo)));
        return vo;
    }

    private long resolveDurationSeconds(AiHostingRecordSessionVo vo) {
        if (vo == null) {
            return 0L;
        }
        Date endTime = firstNonNull(vo.getClosedAt(), new Date());
        if (vo.getOpenedAt() != null) {
            return durationSeconds(vo.getOpenedAt(), endTime);
        }
        return Math.max(0, Objects.requireNonNullElse(vo.getDurationMinutes(), 0)) * 60L;
    }

    private AiHostingRecordSessionVo toVo(DzAutoModeAiHostingRecord record) {
        if (record == null) {
            return null;
        }
        AiHostingRecordSessionVo vo = new AiHostingRecordSessionVo();
        vo.setId(record.getId());
        vo.setRecordName(record.getRecordName());
        vo.setOpenedAt(record.getOpenedAt());
        vo.setClosedAt(record.getClosedAt());
        vo.setDurationMinutes(record.getDurationMinutes());
        vo.setDurationText(record.getDurationText());
        vo.setOpenUserId(record.getOpenUserId());
        vo.setOpenUserName(record.getOpenUserName());
        vo.setOpenUserRole(record.getOpenUserRole());
        vo.setCloseUserId(record.getCloseUserId());
        vo.setCloseUserName(record.getCloseUserName());
        vo.setCloseUserRole(record.getCloseUserRole());
        return fillRuntimeFields(vo);
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private record OperatorSnapshot(Long userId, String userName, String userRole) {

        private static OperatorSnapshot empty() {
            return new OperatorSnapshot(null, null, null);
        }
    }
}
