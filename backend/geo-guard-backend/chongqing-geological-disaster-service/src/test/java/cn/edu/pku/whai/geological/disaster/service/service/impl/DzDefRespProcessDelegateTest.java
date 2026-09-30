/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import cn.edu.pku.whai.geological.disaster.service.consts.enums.DefRespPlanStatusEnum;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DefRespPlan;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.SmsSendSummaryVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.meeting.service.IMeetingService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzProcessProgressService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleApprovalService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskHandleDetailContentService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
class DzDefRespProcessDelegateTest {

    @Test
    void currentOperatorUsesActualLoginUserInsteadOfPlanResponsiblePerson() {
        DzDefRespProcessDelegate delegate = new DzDefRespProcessDelegate(
            mock(DzDefRespPlanServiceImpl.class),
            mock(IDzTaskHandleDetailContentService.class),
            mock(IDzTaskHandleApprovalService.class),
            mock(IDzTaskDistListService.class),
            mock(DzTaskDistListMapper.class),
            mock(IDzProcessProgressService.class),
            mock(IMeetingService.class)
        );
        DefRespPlan plan = new DefRespPlan();
        plan.setId(100L);
        plan.setResponsiblePerson("演示账号");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(200L);
        loginUser.setNickname("县自规局领导");
        loginUser.setUsername("county-leader");

        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::getLoginUser).thenReturn(loginUser);

            DzDefRespProcessDelegate.OperatorSnapshot operator = delegate.currentOperator(plan);

            assertThat(operator.userId()).isEqualTo(200L);
            assertThat(operator.userName()).isEqualTo("县自规局领导");
        }
    }

    @Test
    void currentOperatorFallsBackToUsernameWhenNicknameBlank() {
        DzDefRespProcessDelegate delegate = delegate();
        DefRespPlan plan = new DefRespPlan();
        plan.setId(101L);
        plan.setResponsiblePerson("演示账号");
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(201L);
        loginUser.setNickname("");
        loginUser.setUsername("county-leader");

        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::getLoginUser).thenReturn(loginUser);

            DzDefRespProcessDelegate.OperatorSnapshot operator = delegate.currentOperator(plan);

            assertThat(operator.userId()).isEqualTo(201L);
            assertThat(operator.userName()).isEqualTo("county-leader");
        }
    }

    @Test
    void currentOperatorUsesPlanResponsiblePersonOnlyWhenLoginUnavailable() {
        DzDefRespProcessDelegate delegate = delegate();
        DefRespPlan plan = new DefRespPlan();
        plan.setId(102L);
        plan.setResponsiblePerson("演示账号");

        try (MockedStatic<LoginHelper> loginHelper = mockStatic(LoginHelper.class)) {
            loginHelper.when(LoginHelper::getLoginUser).thenThrow(new IllegalStateException("not login"));

            DzDefRespProcessDelegate.OperatorSnapshot operator = delegate.currentOperator(plan);

            assertThat(operator.userId()).isNull();
            assertThat(operator.userName()).isEqualTo("演示账号");
        }
    }

    @Test
    void autoSendStartSmsOnlyMatchesCountyRegionTransitionFromThreeToFour() {
        DelegateFixture fixture = fixture();
        DefRespPlan plan = new DefRespPlan();
        plan.setId(103L);
        when(fixture.service().isCountyRegionPlan(plan)).thenReturn(true);

        assertThat(fixture.delegate().shouldAutoSendDefRespStartSms(plan,
            DefRespPlanStatusEnum.CONSULTATION_CONFIRMED.getCode(),
            DefRespPlanStatusEnum.APPROVAL_PASSED.getCode())).isTrue();
        assertThat(fixture.delegate().shouldAutoSendDefRespStartSms(plan,
            DefRespPlanStatusEnum.MODEL_ANALYZED.getCode(),
            DefRespPlanStatusEnum.CONSULTATION_CONFIRMED.getCode())).isFalse();
        assertThat(fixture.delegate().shouldAutoSendDefRespStartSms(plan,
            DefRespPlanStatusEnum.APPROVAL_PASSED.getCode(),
            DefRespPlanStatusEnum.TASK_PUBLISHED.getCode())).isFalse();

        when(fixture.service().isCountyRegionPlan(plan)).thenReturn(false);
        assertThat(fixture.delegate().shouldAutoSendDefRespStartSms(plan,
            DefRespPlanStatusEnum.CONSULTATION_CONFIRMED.getCode(),
            DefRespPlanStatusEnum.APPROVAL_PASSED.getCode())).isFalse();
    }

    @Test
    void autoSendStartSmsWaitsUntilTransactionCommit() {
        DelegateFixture fixture = fixture();
        TransactionSynchronizationManager.initSynchronization();
        try {
            fixture.delegate().triggerDefRespStartSmsAfterCommit(104L);

            verifyNoInteractions(fixture.taskDistListService());
            assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
            TransactionSynchronizationManager.getSynchronizations()
                                             .forEach(TransactionSynchronization::afterCommit);

            verify(fixture.taskDistListService(), timeout(2000)).sendDefRespSmsByDefId(104L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void autoSendStartSmsDoesNotRunWhenTransactionRollsBack() {
        DelegateFixture fixture = fixture();
        TransactionSynchronizationManager.initSynchronization();
        try {
            fixture.delegate().triggerDefRespStartSmsAfterCommit(105L);

            TransactionSynchronizationManager.getSynchronizations()
                                             .forEach(synchronization -> synchronization.afterCompletion(
                                                 TransactionSynchronization.STATUS_ROLLED_BACK));

            verifyNoInteractions(fixture.taskDistListService());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void autoSendStartSmsFallsBackToAsyncSubmissionWithoutTransaction() {
        DelegateFixture fixture = fixture();

        fixture.delegate().triggerDefRespStartSmsAfterCommit(106L);

        verify(fixture.taskDistListService(), timeout(2000)).sendDefRespSmsByDefId(106L);
    }

    @Test
    void autoSendStartSmsFailureDoesNotPropagateToStatusFlow() {
        DelegateFixture fixture = fixture();
        doThrow(new ServiceException("短信平台不可用"))
            .when(fixture.taskDistListService()).sendDefRespSmsByDefId(107L);

        assertThatCode(() -> fixture.delegate().sendDefRespStartSmsSafely(107L))
            .doesNotThrowAnyException();
        verify(fixture.taskDistListService()).sendDefRespSmsByDefId(107L);
    }

    @Test
    void approvalRoundOnlyAdvancesForActualTownScopeOrLevelChange() {
        DelegateFixture fixture = fixture();
        DefRespPlan plan = new DefRespPlan();
        plan.setId(108L);
        when(fixture.service().captureTownLevelsForSms(plan))
            .thenReturn(Map.of("龙凤镇", 3, "白杨坪镇", 2));

        assertThat(fixture.delegate().hasActualTownScopeOrLevelChange(plan,
            "[{\"streets\":[\"龙凤镇\"],\"newLevel\":3},{\"streets\":[\"白杨坪镇\"],\"newLevel\":2}]"))
            .isFalse();
        assertThat(fixture.delegate().hasActualTownScopeOrLevelChange(plan,
            "[{\"streets\":[\"龙凤镇\"],\"newLevel\":4},{\"streets\":[\"白杨坪镇\"],\"newLevel\":2}]"))
            .isTrue();
        assertThat(fixture.delegate().hasActualTownScopeOrLevelChange(plan,
            "[{\"streets\":[\"龙凤镇\"],\"newLevel\":3}]"))
            .isTrue();
    }

    private DzDefRespProcessDelegate delegate() {
        return new DzDefRespProcessDelegate(
            mock(DzDefRespPlanServiceImpl.class),
            mock(IDzTaskHandleDetailContentService.class),
            mock(IDzTaskHandleApprovalService.class),
            mock(IDzTaskDistListService.class),
            mock(DzTaskDistListMapper.class),
            mock(IDzProcessProgressService.class),
            mock(IMeetingService.class)
        );
    }

    private DelegateFixture fixture() {
        DzDefRespPlanServiceImpl service = mock(DzDefRespPlanServiceImpl.class);
        IDzTaskDistListService taskDistListService = mock(IDzTaskDistListService.class);
        when(taskDistListService.sendDefRespSmsByDefId(org.mockito.ArgumentMatchers.anyLong()))
            .thenReturn(SmsSendSummaryVo.builder()
                                        .totalCount(1)
                                        .successCount(1)
                                        .failCount(0)
                                        .skipCount(0)
                                        .build());
        DzDefRespProcessDelegate delegate = new DzDefRespProcessDelegate(
            service,
            mock(IDzTaskHandleDetailContentService.class),
            mock(IDzTaskHandleApprovalService.class),
            taskDistListService,
            mock(DzTaskDistListMapper.class),
            mock(IDzProcessProgressService.class),
            mock(IMeetingService.class)
        );
        return new DelegateFixture(delegate, service, taskDistListService);
    }

    private record DelegateFixture(DzDefRespProcessDelegate delegate,
                                   DzDefRespPlanServiceImpl service,
                                   IDzTaskDistListService taskDistListService) {
    }
}
