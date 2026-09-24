package cc.asylum.iridium.web.openapi.config;

import cc.asylum.iridium.config.ConfigurationProperties;
import cc.asylum.iridium.config.Default;

@ConfigurationProperties("iridium.openapi")
public record OpenAPIConfig(@Default("/api-docs") String path) {}
