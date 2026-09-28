package cc.asylum.iridium.config.processor;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.bean.ArgumentBinder;
import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.codegen.Service;
import cc.asylum.iridium.config.Value;

import javax.lang.model.element.VariableElement;
import java.util.Optional;

@Service
public final class ValueBinder implements ArgumentBinder {

  @Override
  public Optional<Expr> bind(final VariableElement parameter, final Processing processing) {
    if (parameter.getAnnotation(Value.class) == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(ConfigBinding.bindValue(processing, parameter));
  }
}
