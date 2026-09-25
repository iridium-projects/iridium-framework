package cc.asylum.iridium.data.dialect;

import org.babyfish.jimmer.sql.dialect.DefaultDialect;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.dialect.MySql5Dialect;
import org.babyfish.jimmer.sql.dialect.MySqlDialect;
import org.babyfish.jimmer.sql.dialect.OracleDialect;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.dialect.SQLiteDialect;
import org.babyfish.jimmer.sql.dialect.SqlServerDialect;
import org.babyfish.jimmer.sql.dialect.TiDBDialect;

import cc.asylum.iridium.core.util.Strings;

import java.util.Locale;

public enum Dialects {
  H2,
  MYSQL,
  MYSQL5,
  POSTGRES,
  ORACLE,
  SQL_SERVER,
  SQLITE,
  TIDB,
  DEFAULT;

  public Dialect create() {
    return switch (this) {
      case H2 -> new H2Dialect();
      case MYSQL -> new MySqlDialect();
      case MYSQL5 -> new MySql5Dialect();
      case POSTGRES -> new PostgresDialect();
      case ORACLE -> new OracleDialect();
      case SQL_SERVER -> new SqlServerDialect();
      case SQLITE -> new SQLiteDialect();
      case TIDB -> new TiDBDialect();
      case DEFAULT -> new OpenDefault();
    };
  }

  public static Dialects parse(final String raw) {
    if (Strings.blank(raw)) {
      throw new IllegalArgumentException("Dialect must not be blank");
    }

    final String key = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');

    return switch (key) {
      case "h2" -> H2;
      case "mysql", "mariadb" -> MYSQL;
      case "mysql5" -> MYSQL5;
      case "postgres", "postgresql", "pg" -> POSTGRES;
      case "oracle" -> ORACLE;
      case "sqlserver", "sql_server", "mssql" -> SQL_SERVER;
      case "sqlite" -> SQLITE;
      case "tidb" -> TIDB;
      case "default" -> DEFAULT;
      default -> throw new IllegalArgumentException("Unknown dialect '" + raw + "'");
    };
  }

  public static Dialects fromUrl(final String url) {
    if (url == null) {
      return DEFAULT;
    }

    final String jdbc = url.toLowerCase(Locale.ROOT);
    if (jdbc.startsWith("jdbc:h2:")) {
      return H2;
    }

    if (jdbc.startsWith("jdbc:mysql:") || jdbc.startsWith("jdbc:mariadb:")) {
      return MYSQL;
    }

    if (jdbc.startsWith("jdbc:postgresql:") || jdbc.startsWith("jdbc:pgsql:")) {
      return POSTGRES;
    }

    if (jdbc.startsWith("jdbc:oracle:")) {
      return ORACLE;
    }

    if (jdbc.startsWith("jdbc:sqlserver:") || jdbc.startsWith("jdbc:microsoft:sqlserver:")) {
      return SQL_SERVER;
    }

    if (jdbc.startsWith("jdbc:sqlite:")) {
      return SQLITE;
    }

    if (jdbc.startsWith("jdbc:tidb:")) {
      return TIDB;
    }

    return DEFAULT;
  }

  private static final class OpenDefault extends DefaultDialect {
  }
}
