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
}
