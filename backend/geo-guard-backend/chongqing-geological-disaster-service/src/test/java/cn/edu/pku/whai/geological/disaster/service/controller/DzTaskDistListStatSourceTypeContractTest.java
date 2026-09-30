/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.controller;

import cn.edu.pku.whai.geological.disaster.service.domain.bo.TaskDistStatStatusBo;
import cn.edu.pku.whai.geological.disaster.service.mapper.DzTaskDistListMapper;
import cn.edu.pku.whai.geological.disaster.service.service.IDzTaskDistListService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class DzTaskDistListStatSourceTypeContractTest {

    @Test
    void controllerExposesSourceTypeStatisticsLikeStatusStatistics() throws Exception {
        Method method = DzTaskDistListController.class.getDeclaredMethod("statSourceType", TaskDistStatStatusBo.class);

        PostMapping mapping = method.getAnnotation(PostMapping.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/stat-source-type");
    }

    @Test
    void serviceAndMapperExposeSourceTypeStatisticsContract() throws Exception {
        Method serviceMethod = IDzTaskDistListService.class.getDeclaredMethod("statSourceType", TaskDistStatStatusBo.class);
        Method mapperMethod = DzTaskDistListMapper.class.getDeclaredMethod("statSourceType", QueryWrapper.class);

        assertThat(serviceMethod.getGenericReturnType().getTypeName()).contains("TaskDistStatSourceTypeVo");
        assertThat(mapperMethod.getGenericReturnType().getTypeName()).contains("TaskDistStatSourceTypeVo");
    }

    @Test
    void mapperStatisticsSqlUsesSourceTypeAndCount() throws Exception {
        Method mapperMethod = DzTaskDistListMapper.class.getDeclaredMethod("statSourceType", QueryWrapper.class);

        String sql = String.join("\n", mapperMethod.getAnnotation(Select.class).value()).toLowerCase();

        assertThat(sql).contains("source_type");
        assertThat(sql).contains("count(*)");
        assertThat(sql).contains("${ew.customsqlsegment}");
    }

    @Test
    void sourceTypeStatisticsVoCarriesSourceTypeAndCount() throws Exception {
        Class<?> voClass = Class.forName("cn.edu.pku.whai.geological.disaster.service.domain.vo.TaskDistStatSourceTypeVo");

        assertThat(voClass.getDeclaredField("sourceType").getType()).isEqualTo(Integer.class);
        assertThat(voClass.getDeclaredField("count").getType()).isEqualTo(Integer.class);
    }

    @Test
    void statisticsRequestSupportsExplicitTimeRange() throws Exception {
        assertThat(TaskDistStatStatusBo.class.getDeclaredField("beginTime").getType()).isEqualTo(Date.class);
        assertThat(TaskDistStatStatusBo.class.getDeclaredField("endTime").getType()).isEqualTo(Date.class);
    }
}
