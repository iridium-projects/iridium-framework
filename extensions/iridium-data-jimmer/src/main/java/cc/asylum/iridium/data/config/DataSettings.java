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
    @Default("NONE") Validation validation,
    @Default("UPPER") Naming naming,
    @Default("") String schema,
    @Default("") String catalog,
    @Nullable Pool pool
) {

  public enum Validation {
    NONE,
    WARNING,
    ERROR
  }

  public enum Naming {
    UPPER,
    LOWER
  }

  public record Pool(
      @Default("10") int maximumPoolSize,
      @Default("1") int minimumIdle,
      @Default("30000") long connectionTimeout,
      @Default("600000") long idleTimeout,
      @Default("1800000") long maxLifetime,
      @Default("iridium") String poolName
  ) {
  }

  public static DataSettings of(final String url) {
    return new DataSettings(
        url,
        "",
        "",
        "",
        false,
        false,
        Validation.NONE,
        Naming.UPPER,
        "",
        "",
        new Pool(10, 1, 30_000L, 600_000L, 1_800_000L, "iridium")
    );
  }
}
