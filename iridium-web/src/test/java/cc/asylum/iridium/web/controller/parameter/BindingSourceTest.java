package cc.asylum.iridium.web.controller.parameter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BindingSourceTest {

  @Test
  void exposesEverySource() {
    assertEquals(5, BindingSource.values().length);
    assertEquals(BindingSource.PATH, BindingSource.valueOf("PATH"));
    assertEquals(BindingSource.BODY, BindingSource.valueOf("BODY"));
  }
}
