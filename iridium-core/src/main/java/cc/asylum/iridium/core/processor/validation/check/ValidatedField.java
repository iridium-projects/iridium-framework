package cc.asylum.iridium.core.processor.validation.check;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.core.annotation.Internal;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

@Internal
public record ValidatedField(String name, TypeMirror type, Element element, Expr accessor) {
}
