package cc.asylum.iridium.config.processor;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.model.Elements;
import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.config.Default;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.regex.Pattern;
import cc.asylum.iridium.config.processor.walk.ConfigWalk;

public final class ConfigBinding {

  public static final Pattern PLACEHOLDER = Pattern.compile("^\\$\\{([^}:]+)(?::(.*))?}$");

  private ConfigBinding() {
  }

  public static Expr bindRoot(final Processing context, final TypeElement type) {
    return new ConfigWalk(context).bindRoot(type);
  }

  public static Expr bindValue(final Processing context, final VariableElement parameter) {
    return new ConfigWalk(context).bindValue(parameter);
  }

  public static String defaultOf(final VariableElement parameter, final RecordComponentElement component) {
    final Default annotation = annotation(parameter, component, Default.class);
    return annotation == null ? null : annotation.value();
  }

  public static ExecutableElement constructorOf(final TypeElement type) {
    if (type.getKind() != ElementKind.RECORD) {
      return Elements.constructor(type);
    }
    final int count = type.getRecordComponents().size();
    for (final Element enclosed : type.getEnclosedElements()) {
      if (enclosed.getKind() == ElementKind.CONSTRUCTOR
          && ((ExecutableElement) enclosed).getParameters().size() == count) {
        return (ExecutableElement) enclosed;
      }
    }
    return null;
  }

  public static <A extends Annotation> A annotation(
      final VariableElement parameter,
      final RecordComponentElement component,
      final Class<A> type
  ) {
    final A onParameter = parameter.getAnnotation(type);
    if (onParameter != null) {
      return onParameter;
    }
    return component == null ? null : component.getAnnotation(type);
  }

  public static boolean validPrefix(final String prefix) {
    if (prefix.isEmpty()) {
      return true;
    }
    if (prefix.startsWith(".") || prefix.endsWith(".") || prefix.contains("..")) {
      return false;
    }
    for (int i = 0; i < prefix.length(); i++) {
      final char c = prefix.charAt(i);
      if (!Character.isLetterOrDigit(c) && c != '.' && c != '-' && c != '_') {
        return false;
      }
    }
    return true;
  }
}
