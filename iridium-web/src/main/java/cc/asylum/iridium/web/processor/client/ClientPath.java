package cc.asylum.iridium.web.processor.client;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.forgery.model.TypeRef;
import cc.asylum.iridium.codegen.code.Exprs;
import cc.asylum.iridium.codegen.code.Types;
import cc.asylum.iridium.core.annotation.Internal;

import java.util.ArrayList;
import java.util.List;

@Internal
public final class ClientPath {

  static final TypeRef CLIENTS = Types.of("cc.asylum.iridium.web.undertow.client", "UndertowClients");

  private ClientPath() {
  }

  public static Expr expression(
    final String path,
    final List<String> pathArgs,
    final List<String> query) {
    final List<Expr> parts = new ArrayList<>();

    int cursor = 0;
    while (cursor < path.length()) {
      final int open = path.indexOf('{', cursor);
      if (open < 0) {
        parts.add(Expr.lit(path.substring(cursor)));
        break;
      }

      final int close = path.indexOf('}', open);
      if (close < 0) {
        parts.add(Expr.lit(path.substring(cursor)));
        break;
      }

      if (open > cursor) {
        parts.add(Expr.lit(path.substring(cursor, open)));
      }

      parts.add(argument(path.substring(open + 1, close), pathArgs));
      cursor = close + 1;
    }

    if (parts.isEmpty()) {
      parts.add(Expr.lit(path));
    }

    boolean first = true;
    for (int i = 0; i < query.size(); i += 2) {
      parts.add(Expr.lit((first ? "?" : "&") + query.get(i) + "="));
      parts.add(Exprs.invokeStatic(CLIENTS, "query", Exprs.name(query.get(i + 1))));
      first = false;
    }

    return Exprs.concat(parts);
  }

  private static Expr argument(final String name, final List<String> args) {
    for (int i = 0; i < args.size(); i += 2) {
      if (args.get(i).equals(name)) {
        return Exprs.name(args.get(i + 1));
      }
    }

    return Expr.lit("");
  }
}
