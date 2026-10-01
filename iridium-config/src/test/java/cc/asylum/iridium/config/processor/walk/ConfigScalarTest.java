package cc.asylum.iridium.config.processor.walk;

import cc.asylum.forgery.expr.Expr;
import cc.asylum.iridium.codegen.Processing;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class ConfigScalarTest {

  @Test
  void readsScalarsWithAndWithoutFallback() {
    final Processing processing = processing();
    final Element element = mock(VariableElement.class);
    final ConfigKey key = ConfigKey.literal("app.count");
    assertNotNull(ConfigScalar.simple("string", Expr.nil()).read(processing, element, key, null, false));
    assertNotNull(ConfigScalar.simple("string", Expr.nil()).read(processing, element, key, null, true));
    assertNotNull(ConfigScalar.simple("integer", Expr.nil()).read(processing, element, key, null, true));
    assertNotNull(ConfigScalar.simple("integer", Expr.nil()).read(processing, element, key, null, false));
    assertNotNull(ConfigScalar.simple("string", Expr.nil()).read(processing, element, key, "text", false));
    assertNotNull(ConfigScalar.simple("bool", Expr.nil()).read(processing, element, key, "true", false));
    assertNotNull(ConfigScalar.simple("integer", Expr.nil()).read(processing, element, key, " 7 ", false));
    assertNotNull(ConfigScalar.simple("longValue", Expr.nil()).read(processing, element, key, "9", false));
    assertNotNull(ConfigScalar.simple("doubleValue", Expr.nil()).read(processing, element, key, "1.5", false));
    assertNotNull(ConfigScalar.simple("floatValue", Expr.nil()).read(processing, element, key, "1.25", false));
    assertNotNull(ConfigScalar.simple("shortValue", Expr.nil()).read(processing, element, key, "2", false));
    assertNotNull(ConfigScalar.simple("byteValue", Expr.nil()).read(processing, element, key, "1", false));
    assertNotNull(ConfigScalar.simple("bool", Expr.nil()).read(processing, element, key, "maybe", false));
    assertNull(ConfigScalar.simple("bool", Expr.nil()).read(processing, element, key, "   ", false));
    assertNull(ConfigScalar.simple("integer", Expr.nil()).read(processing, element, key, "nope", false));
    assertNull(ConfigScalar.simple("other", Expr.nil()).read(processing, element, key, "x", false));
    verify(processing.messager()).printMessage(
        javax.tools.Diagnostic.Kind.ERROR,
        "invalid default 'nope'",
        element);
  }

  @Test
  void readsEnumerations() {
    final Processing processing = processing();
    final Element element = mock(VariableElement.class);
    final TypeElement enumeration = enumType("pkg.Mode", "DEV", "QA");
    final ConfigScalar scalar = ConfigScalar.enumeration(enumeration);
    final ConfigKey key = ConfigKey.literal("app.mode");
    assertNotNull(scalar.read(processing, element, key, null, false));
    assertNotNull(scalar.read(processing, element, key, null, true));
    assertNotNull(scalar.read(processing, element, key, "DEV", false));
    assertNull(scalar.read(processing, element, key, "MISSING", false));
    verify(processing.messager()).printMessage(
        javax.tools.Diagnostic.Kind.ERROR,
        "invalid default 'MISSING'",
        element);
  }

  private static Processing processing() {
    return new Processing(
        mock(javax.lang.model.util.Types.class),
        mock(javax.lang.model.util.Elements.class),
        mock(Messager.class),
        mock(javax.annotation.processing.Filer.class),
        mock(javax.annotation.processing.RoundEnvironment.class));
  }

  private static TypeElement enumType(final String qualified, final String... constants) {
    final TypeElement type = mock(TypeElement.class);
    final Name name = mock(Name.class);
    when(name.toString()).thenAnswer(invocation -> qualified);
    when(type.getQualifiedName()).thenAnswer(invocation -> name);
    when(type.getKind()).thenAnswer(invocation -> ElementKind.ENUM);
    final List<Element> enclosed = new java.util.ArrayList<>();
    for (final String constant : constants) {
      final VariableElement field = mock(VariableElement.class);
      final Name simple = mock(Name.class);
      when(simple.contentEquals(constant)).thenAnswer(invocation -> true);
      when(simple.contentEquals(org.mockito.ArgumentMatchers.anyString())).thenAnswer(invocation -> constant.equals(invocation.getArgument(0)));
      when(field.getSimpleName()).thenAnswer(invocation -> simple);
      when(field.getKind()).thenAnswer(invocation -> ElementKind.ENUM_CONSTANT);
      enclosed.add(field);
    }
    final Element method = mock(Element.class);
    when(method.getKind()).thenAnswer(invocation -> ElementKind.METHOD);
    enclosed.add(method);
    when(type.getEnclosedElements()).thenAnswer(invocation -> enclosed);
    return type;
  }
}
