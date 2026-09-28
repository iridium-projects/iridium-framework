package cc.asylum.iridium.data.config;

import cc.asylum.iridium.config.ConfigurationProperties;
import cc.asylum.iridium.config.Default;
import cc.asylum.iridium.core.validation.annotation.Nullable;

@ConfigurationProperties("iridium.data")
public record DataSettings(
    String url,
    @Default("") String username,
    @Default("") String password,
    @Default("") String dialect,
    @Default("false") boolean showSql,
    @Default("false") boolean prettySql,
    @Default("NONE") ValidationMode validation,
    @Default("UPPER") NamingMode naming,
    @Default("") String schema,
    @Default("") String catalog,
    @Nullable PoolSettings pool
) {

  public static DataSettings of(final String url) {
    return new DataSettings(
        url,
        "",
        "",
        "",
        false,
        false,
        ValidationMode.NONE,
        NamingMode.UPPER,
        "",
        "",
        new PoolSettings(10, 1, 30_000L, 600_000L, 1_800_000L, "iridium")
    );
  }
}
