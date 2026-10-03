package cc.asylum.iridium.web.processor.binding.convert;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.core.annotation.Internal;

@Internal
public interface RequestValues {

  Expr one(
    final boolean header,
    final String name,
    final Expr fallback);

  Expr many(final boolean header, final String name);

  Expr invalid(final Expr raw, final String param);
}
