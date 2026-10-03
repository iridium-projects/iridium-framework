package cc.asylum.iridium.config.processor.binding;

import cc.asylum.forgery.expr.Expr;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigBoundTest {

  @Test
  void exposesComponents() {
    final Expr code = Expr.lit("key");
    final ConfigBound bound = new ConfigBound(code, true);
    assertEquals(code, bound.code());
    assertTrue(bound.result());
    assertFalse(new ConfigBound(code, false).result());
  }
}
