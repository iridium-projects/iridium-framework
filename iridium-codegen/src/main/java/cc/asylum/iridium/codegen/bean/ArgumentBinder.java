package cc.asylum.iridium.codegen.bean;

import cc.asylum.iridium.codegen.Processing;

import cc.asylum.forgery.expr.Expr;

import javax.lang.model.element.VariableElement;
import java.util.Optional;

public interface ArgumentBinder {

  Optional<Expr> bind(final VariableElement parameter, final Processing processing);
}
