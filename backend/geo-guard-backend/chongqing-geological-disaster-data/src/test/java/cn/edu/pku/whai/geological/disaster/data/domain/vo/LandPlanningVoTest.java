/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.domain.vo;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class LandPlanningVoTest {

    @Test
    void onlyExposesLandNameAndIntersectionArea() {
        Set<String> instanceFields = Arrays.stream(LandPlanningVo.class.getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .map(field -> field.getName())
            .collect(Collectors.toSet());

        assertThat(instanceFields).containsExactlyInAnyOrder("landName", "intersectionArea");
    }
}
