/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.service.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class DzTaskHandleSceneRecordServiceImplTest {

    @Test
    void normalizePhotoPanoramaExtractsOssIdFromUrl() {
        String result = DzTaskHandleSceneRecordServiceImpl.normalizePhotoPanorama(
            "https://223.75.53.123:9828/dzzh-xqsc/v1/tasks/feedback/photos/2070819841670344705"
        );

        assertThat(result).isEqualTo("2070819841670344705");
    }

    @Test
    void normalizePhotoPanoramaKeepsPlainOssId() {
        String result = DzTaskHandleSceneRecordServiceImpl.normalizePhotoPanorama("2070819841670344705");

        assertThat(result).isEqualTo("2070819841670344705");
    }

    @Test
    void normalizePhotoPanoramaSupportsCommaSeparatedUrlAndIdValues() {
        String result = DzTaskHandleSceneRecordServiceImpl.normalizePhotoPanorama(
            " https://223.75.53.123:9828/dzzh-xqsc/v1/tasks/feedback/photos/2070819841670344705,"
                + "2070820177260802049,"
                + " https://223.75.53.123:9828/dzzh-xqsc/v1/tasks/feedback/photos/2070820478269222913 "
        );

        assertThat(result).isEqualTo("2070819841670344705,2070820177260802049,2070820478269222913");
    }
}
