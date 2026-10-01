/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.utils;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static cn.edu.pku.whai.geological.disaster.data.utils.GeoDirectionUtil.azimuthDegreesTo8Dir;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * GeoDirectionUtil 8 方位分类边界测试
 *
 * @author zhuzc
 * @date 2026-06-17
 */
@Tag("dev")
class GeoDirectionUtilTest {

    @Test
    void zeroDegreesIsNorth() {
        assertThat(azimuthDegreesTo8Dir(0.0)).isEqualTo("N");
    }

    @Test
    void near360IsNorth() {
        assertThat(azimuthDegreesTo8Dir(359.99)).isEqualTo("N");
    }

    @Test
    void lowerBoundOfN() {
        assertThat(azimuthDegreesTo8Dir(22.4)).isEqualTo("N");
    }

    @Test
    void upperBoundOfNToNe() {
        assertThat(azimuthDegreesTo8Dir(22.5)).isEqualTo("NE");
    }

    @Test
    void northeast() {
        assertThat(azimuthDegreesTo8Dir(45.0)).isEqualTo("NE");
    }

    @Test
    void east() {
        assertThat(azimuthDegreesTo8Dir(90.0)).isEqualTo("E");
    }

    @Test
    void southeast() {
        assertThat(azimuthDegreesTo8Dir(135.0)).isEqualTo("SE");
    }

    @Test
    void south() {
        assertThat(azimuthDegreesTo8Dir(180.0)).isEqualTo("S");
    }

    @Test
    void southwest() {
        assertThat(azimuthDegreesTo8Dir(225.0)).isEqualTo("SW");
    }

    @Test
    void west() {
        assertThat(azimuthDegreesTo8Dir(270.0)).isEqualTo("W");
    }

    @Test
    void northwest() {
        assertThat(azimuthDegreesTo8Dir(315.0)).isEqualTo("NW");
    }

    @Test
    void negativeNormalized() {
        assertThat(azimuthDegreesTo8Dir(-90.0)).isEqualTo("W");
    }

    @Test
    void over360Normalized() {
        assertThat(azimuthDegreesTo8Dir(720.0)).isEqualTo("N");
    }
}
