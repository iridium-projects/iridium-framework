package cc.asylum.iridium.core.processor.validation.check;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.code.Exprs;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

final class ViolationsTest {

  @Test
  void addAndWhenBuildStatements() {
    final ValidatedField field = new ValidatedField("name", mock(TypeMirror.class), mock(Element.class), Exprs.name("name"));
    assertNotNull(Violations.add(Expr.lit("name"), Expr.lit("bad"), Expr.nil()));
    assertNotNull(Violations.when(Expr.lit(true), field, "bad", Expr.nil()));
    assertNotNull(Violations.LIST);
    assertNotNull(Violations.VALUE);
  }
}
