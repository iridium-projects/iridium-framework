package cc.asylum.iridium.web.processor.binding.convert;

import cc.asylum.forgery.expr.Expr;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

final class WebRequestValuesTest {

  @Test
  void readsOneManyAndInvalid() {
    final WebRequestValues values = new WebRequestValues();
    assertNotNull(values.one(true, "auth", Expr.nil()));
    assertNotNull(values.one(false, "q", Expr.lit("x")));
    assertNotNull(values.many(true, "auth"));
    assertNotNull(values.many(false, "q"));
    assertNotNull(values.invalid(null, "q"));
    assertNotNull(values.invalid(Expr.lit("bad"), "q"));
  }
}
