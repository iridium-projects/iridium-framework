package cc.asylum.iridium.config.processor.binding;

import cc.asylum.forgery.expr.Expr;

public record ConfigPiece(Expr code, boolean result, String name) {
}
