package cc.asylum.iridium.data.dialect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DialectsTest {

  @Test
  void parsesAliasesAndInfersFromUrl() {
    assertEquals(Dialects.POSTGRES, Dialects.parse("PostgreSQL"));
    assertEquals(Dialects.SQL_SERVER, Dialects.parse("sql-server"));
    assertEquals(Dialects.MYSQL, Dialects.parse("mariadb"));
    assertEquals(Dialects.H2, Dialects.fromUrl("jdbc:h2:mem:test"));
    assertEquals(Dialects.SQLITE, Dialects.fromUrl("jdbc:sqlite:file.db"));
    assertEquals(Dialects.DEFAULT, Dialects.fromUrl("jdbc:unknown:db"));
    assertInstanceOf(org.babyfish.jimmer.sql.dialect.PostgresDialect.class, Dialects.POSTGRES.create());
    assertThrows(IllegalArgumentException.class, () -> Dialects.parse("nope"));
  }

  @Test
  void coversAliasesUrlsAndBlankInput() {
    assertEquals(Dialects.MYSQL5, Dialects.parse("mysql5"));
    assertEquals(Dialects.POSTGRES, Dialects.parse("pg"));
    assertEquals(Dialects.SQL_SERVER, Dialects.parse("mssql"));
    assertEquals(Dialects.TIDB, Dialects.parse("TiDB"));
    assertEquals(Dialects.DEFAULT, Dialects.parse("default"));
    assertEquals(Dialects.ORACLE, Dialects.parse(" oracle "));
    assertThrows(IllegalArgumentException.class, () -> Dialects.parse(null));
    assertThrows(IllegalArgumentException.class, () -> Dialects.parse("  "));
    assertEquals(Dialects.DEFAULT, Dialects.fromUrl(null));
    assertEquals(Dialects.MYSQL, Dialects.fromUrl("jdbc:mariadb://localhost/db"));
    assertEquals(Dialects.POSTGRES, Dialects.fromUrl("jdbc:pgsql:db"));
    assertEquals(Dialects.ORACLE, Dialects.fromUrl("jdbc:oracle:thin:@localhost:1521:xe"));
    assertEquals(Dialects.SQL_SERVER, Dialects.fromUrl("jdbc:microsoft:sqlserver://localhost"));
    assertEquals(Dialects.TIDB, Dialects.fromUrl("jdbc:tidb://localhost/db"));
    for (final Dialects dialect : Dialects.values()) {
      assertEquals(dialect.create().getClass(), dialect.create().getClass());
    }
  }
}
