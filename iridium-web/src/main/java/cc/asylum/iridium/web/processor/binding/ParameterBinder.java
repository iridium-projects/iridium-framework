package cc.asylum.iridium.web.processor.binding;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.write.Body;
import cc.asylum.iridium.core.annotation.Internal;

import javax.lang.model.element.VariableElement;
import java.util.Optional;

@Internal
public interface ParameterBinder {

  boolean matches(final VariableElement parameter);

  Optional<Expr> emit(final Body body, final VariableElement parameter);
}
