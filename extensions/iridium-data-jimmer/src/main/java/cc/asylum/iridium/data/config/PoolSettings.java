package cc.asylum.iridium.data.config;

import cc.asylum.iridium.config.Default;

public record PoolSettings(
    @Default("10") int maximumPoolSize,
    @Default("1") int minimumIdle,
    @Default("30000") long connectionTimeout,
    @Default("600000") long idleTimeout,
    @Default("1800000") long maxLifetime,
    @Default("iridium") String poolName
) {
}
