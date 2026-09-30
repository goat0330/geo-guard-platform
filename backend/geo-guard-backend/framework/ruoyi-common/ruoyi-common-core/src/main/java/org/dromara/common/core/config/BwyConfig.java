package org.dromara.common.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Public service identity settings retained from the original backend. */
@Data
@Component
@ConfigurationProperties(prefix = "bwy")
public class BwyConfig {

    private String name = "geo-guard-backend";

    private String version = "development";

    private Integer copyrightYear;
}
