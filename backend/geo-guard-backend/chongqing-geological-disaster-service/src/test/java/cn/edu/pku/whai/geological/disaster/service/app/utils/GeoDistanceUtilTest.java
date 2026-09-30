/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.app.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class GeoDistanceUtilTest {

    @Test
    void parsePointShouldSupportPointAndPointZ() {
        assertThat(GeoDistanceUtil.parsePoint("POINT(109.22075655175031 30.399669964703445)"))
            .containsExactly(109.22075655175031, 30.399669964703445);

        assertThat(GeoDistanceUtil.parsePoint("POINT Z (109.22075655175031 30.399669964703445 792.38)"))
            .containsExactly(109.22075655175031, 30.399669964703445);
    }

    @Test
    void parsePointShouldReturnNullWhenCoordinateIsInvalid() {
        assertThat(GeoDistanceUtil.parsePoint("POINT(109.2207565517503130.399669964703445)")).isNull();
        assertThat(GeoDistanceUtil.parsePoint("bad-coordinate")).isNull();
    }
}
