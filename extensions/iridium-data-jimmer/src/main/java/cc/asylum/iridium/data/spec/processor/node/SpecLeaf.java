package cc.asylum.iridium.data.spec.processor.node;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.core.util.Lists;

import javax.lang.model.type.TypeMirror;
import java.util.List;

public record SpecLeaf(
    Expr expr,
    TypeMirror type,
    String op,
    List<String> params,
    boolean header,
    String constVal,
    String defaultVal,
    boolean not,
    boolean collection
) implements SpecNode {

  public String param() {
    final String first = Lists.first(params);
    return first == null ? "" : first;
  }
}
