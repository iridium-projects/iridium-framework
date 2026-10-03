package cc.asylum.iridium.config.source;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class KeysTest {

  @Test
  void joinHandlesEmptySides() {
    assertEquals("name", Keys.join("", "name"));
    assertEquals("name", Keys.join(null, "name"));
    assertEquals("server", Keys.join("server", ""));
    assertEquals("server", Keys.join("server", null));
    assertEquals("server.port", Keys.join("server", "port"));
  }

  @Test
  void canonicalDropsSeparatorsAndLowersCase() {
    assertEquals("serverport[0]", Keys.canonical("Server-Port_[0]"));
    assertEquals("server.port", Keys.canonical("Server.Port"));
    assertEquals("", Keys.canonical("-_"));
  }

  @Test
  void envCandidatesPreferSeparatedThenCompact() {
    assertEquals(List.of("SERVER_PORT"), Keys.envCandidates("server.port"));
    assertEquals(List.of("SERVER_PORT", "SERVERPORT"), Keys.envCandidates("server-port"));
    assertEquals(List.of("APP_NAME", "APPNAME"), Keys.envCandidates("appName"));
    assertEquals(List.of("ALREADY_SPLIT"), Keys.envCandidates("already_split"));
  }

  @Test
  void separateCollapsesRepeatedDelimitersAndSplitsCamelCase() {
    assertEquals("server_name", Keys.separate("server..name"));
    assertEquals("_leading", Keys.separate(".leading"));
    assertEquals("already_split", Keys.separate("already_split"));
    assertEquals("H_T_T_P_Server", Keys.separate("HTTPServer"));
  }
}
