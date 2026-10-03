package cc.asylum.iridium.codegen.code;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NamesTest {

  @Test
  void prefixesAreCountedIndependently() {
    final Names names = new Names();
    assertEquals("arg0", names.next("arg"));
    assertEquals("arg1", names.next("arg"));
    assertEquals("tmp0", names.next("tmp"));
  }
}
