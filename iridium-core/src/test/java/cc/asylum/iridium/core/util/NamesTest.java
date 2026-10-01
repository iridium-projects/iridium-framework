package cc.asylum.iridium.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class NamesTest {

  @Test
  void binaryConvertsPaths() {
    assertNull(Names.binary(null));
    assertEquals("cc.asylum.Names", Names.binary("cc/asylum/Names.class"));
    assertEquals("cc.asylum.Names", Names.binary("cc\\asylum\\Names"));
    assertEquals("Names", Names.binary("Names"));
  }
}
