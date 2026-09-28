package cc.asylum.iridium.data;

import cc.asylum.iridium.data.config.DataSettings;
import cc.asylum.iridium.data.config.NamingMode;
import cc.asylum.iridium.data.config.PoolSettings;
import cc.asylum.iridium.data.config.ValidationMode;
import cc.asylum.iridium.data.dialect.Dialects;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.babyfish.jimmer.sql.runtime.DatabaseValidationMode;
import org.babyfish.jimmer.sql.runtime.DefaultDatabaseNamingStrategy;
import org.babyfish.jimmer.sql.runtime.Executor;
import org.babyfish.jimmer.sql.runtime.SqlFormatter;

import javax.sql.DataSource;
import java.util.List;

@Slf4j
public final class DataClients {

  private DataClients() {
  }

  public static DataSource dataSource(final DataSettings settings) {
    final HikariConfig config = new HikariConfig();
    config.setJdbcUrl(settings.url());

    if (!settings.username().isBlank()) {
      config.setUsername(settings.username());
    }

    if (!settings.password().isBlank()) {
      config.setPassword(settings.password());
    }

    if (!settings.schema().isBlank()) {
      config.setSchema(settings.schema());
    }

    if (!settings.catalog().isBlank()) {
      config.setCatalog(settings.catalog());
    }

    final PoolSettings pool = settings.pool() == null
        ? new PoolSettings(10, 1, 30_000L, 600_000L, 1_800_000L, "iridium")
        : settings.pool();

    config.setMaximumPoolSize(pool.maximumPoolSize());
    config.setMinimumIdle(Math.min(pool.minimumIdle(), pool.maximumPoolSize()));
    config.setConnectionTimeout(pool.connectionTimeout());
    config.setIdleTimeout(pool.idleTimeout());
    config.setMaxLifetime(pool.maxLifetime());
    config.setPoolName(pool.poolName());

    return new HikariDataSource(config);
  }

  public static JSqlClient sqlClient(final DataSource dataSource, final DataSettings settings) {
    final JSqlClient.Builder builder = JSqlClient.newBuilder()
        .setConnectionManager(ConnectionManager.simpleConnectionManager(dataSource))
        .setDialect(dialect(settings).create())
        .setDatabaseValidationMode(validation(settings.validation()))
        .setDatabaseNamingStrategy(settings.naming() == NamingMode.LOWER
            ? DefaultDatabaseNamingStrategy.LOWER_CASE
            : DefaultDatabaseNamingStrategy.UPPER_CASE);

    if (settings.showSql()) {
      builder.setExecutor(Executor.log());
      builder.setExecutorContextPrefixes(List.of("cc.asylum"));
    }

    if (settings.prettySql()) {
      builder.setSqlFormatter(SqlFormatter.PRETTY);
    }

    return builder.build();
  }

  private static Dialects dialect(final DataSettings settings) {
    if (settings.dialect() == null || settings.dialect().isBlank()) {
      return Dialects.fromUrl(settings.url());
    }
    return Dialects.parse(settings.dialect());
  }

  private static DatabaseValidationMode validation(final ValidationMode validation) {
    return switch (validation) {
      case NONE -> DatabaseValidationMode.NONE;
      case WARNING -> DatabaseValidationMode.WARNING;
      case ERROR -> DatabaseValidationMode.ERROR;
    };
  }
}
