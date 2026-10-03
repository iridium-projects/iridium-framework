package cc.asylum.iridium.web.processor.binding.convert;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.web.controller.parameter.BindingSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

final class RequestReadExprTest {

  @Test
  void readsEverySource() {
    assertNotNull(RequestRead.REQUEST);
    final Expr fallback = Expr.lit("fallback");
    assertNotNull(RequestRead.raw(BindingSource.BODY, "body", fallback));
    assertNotNull(RequestRead.raw(BindingSource.PATH, "id", fallback));
    assertNotNull(RequestRead.raw(BindingSource.HEADER, "auth", fallback));
    assertNotNull(RequestRead.raw(BindingSource.COOKIE, "sid", fallback));
    assertNotNull(RequestRead.raw(BindingSource.QUERY, "q", fallback));
    assertNotNull(RequestRead.badRequest(Expr.lit("nope")));
  }
}
