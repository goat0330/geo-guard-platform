/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.thirdparty.warning;

import cn.edu.pku.whai.geological.disaster.service.domain.bo.ThirdPartyWarningRiskIncreaseSimulateBo;
import cn.edu.pku.whai.geological.disaster.service.domain.po.DzRiskAssessment;
import cn.edu.pku.whai.geological.disaster.service.domain.vo.ThirdPartyWarningRiskIncreaseSimulateVo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzRiskAssessmentMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzAutomationService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzRiskAssessmentWarningRelationService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskProcessChainSummaryService;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class ThirdPartyWarningRiskLinkageServiceTest {

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(DzRiskAssessment.class) == null) {
            TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                DzRiskAssessment.class
            );
        }
    }

    @Test
    void simulateRiskIncreaseSyncsRiskLevelWithDynamicRiskLevel() {
        DzRiskAssessmentMapper riskAssessmentMapper = mock(DzRiskAssessmentMapper.class);
        IDzTaskProcessChainSummaryService taskProcessChainSummaryService = mock(IDzTaskProcessChainSummaryService.class);
        DzRiskAssessment assessment = new DzRiskAssessment();
        assessment.setId(1L);
        assessment.setSlopeUnitId("0001");
        assessment.setCreateDate(new Date());
        assessment.setDynamicRiskLevel(1);
        assessment.setRiskLevel(1);
        when(riskAssessmentMapper.selectOne(any())).thenReturn(assessment);
        when(riskAssessmentMapper.update(isNull(), any())).thenReturn(1);

        ThirdPartyWarningRiskIncreaseSimulateBo bo = new ThirdPartyWarningRiskIncreaseSimulateBo();
        bo.setSlopeUnitId("1");
        bo.setIncreaseLevel(1);

        ThirdPartyWarningRiskIncreaseSimulateVo result = service(riskAssessmentMapper, taskProcessChainSummaryService)
            .simulateRiskIncrease(bo);

        ArgumentCaptor<Wrapper<DzRiskAssessment>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(riskAssessmentMapper).update(isNull(), captor.capture());
        verify(taskProcessChainSummaryService).refreshDynamicRiskLevelByRiskAssessment(1L);
        assertThat(result.getUpdatedDynamicRiskLevel()).isEqualTo(2);
        assertThat(captor.getValue()).isInstanceOf(LambdaUpdateWrapper.class);
        assertThat(((LambdaUpdateWrapper<DzRiskAssessment>) captor.getValue()).getSqlSet())
            .contains("dynamic_risk_level")
            .contains("risk_level");
    }

    private ThirdPartyWarningRiskLinkageService service(DzRiskAssessmentMapper riskAssessmentMapper,
                                                        IDzTaskProcessChainSummaryService taskProcessChainSummaryService) {
        ThirdPartyWarningRiskLinkageProps props = new ThirdPartyWarningRiskLinkageProps();
        return new ThirdPartyWarningRiskLinkageService(
            riskAssessmentMapper,
            mock(IDzRiskAssessmentWarningRelationService.class),
            mock(IDzTaskDistListService.class),
            taskProcessChainSummaryService,
            mock(IDzAutomationService.class),
            props,
            mock(NamedParameterJdbcTemplate.class)
        );
    }
}
