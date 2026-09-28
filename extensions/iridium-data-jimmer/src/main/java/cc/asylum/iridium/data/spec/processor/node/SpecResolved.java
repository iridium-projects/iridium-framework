package cc.asylum.iridium.data.spec.processor.node;

import cc.asylum.forgery.expr.Expr;

import javax.lang.model.type.TypeMirror;

public record SpecResolved(Expr expr, TypeMirror type) {
}
