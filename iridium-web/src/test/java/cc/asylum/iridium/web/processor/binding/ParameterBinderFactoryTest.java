package cc.asylum.iridium.web.processor.binding;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.write.Body;
import cc.asylum.iridium.web.processor.binding.convert.RequestValues;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Messager;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

final class ParameterBinderFactoryTest {

  @Test
  void createsBinder() {
    final ParameterBinder binder = new ParameterBinder() {
      @Override
      public boolean matches(final VariableElement parameter) {
        return false;
      }

      @Override
      public Optional<Expr> emit(final Body body, final VariableElement parameter) {
        return Optional.empty();
      }
    };
    final ParameterBinderFactory factory = (types, elements, messager, values) -> binder;
    assertSame(binder, factory.create(mock(Types.class), mock(Elements.class), mock(Messager.class), mock(RequestValues.class)));
  }
}
