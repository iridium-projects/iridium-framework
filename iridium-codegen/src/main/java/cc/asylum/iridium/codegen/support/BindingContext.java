package cc.asylum.iridium.codegen.support;

import cc.asylum.iridium.core.annotation.Internal;

import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

@Internal
public final class BindingContext {

  public final Types types;
  public final Elements elements;
  public final Messager messager;

  public BindingContext(final Types types, final Elements elements, final Messager messager) {
    this.types = types;
    this.elements = elements;
    this.messager = messager;
  }
}
