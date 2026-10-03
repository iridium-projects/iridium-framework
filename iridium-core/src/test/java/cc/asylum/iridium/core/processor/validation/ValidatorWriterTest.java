package cc.asylum.iridium.core.processor.validation;

import cc.asylum.iridium.core.processor.Mirrors;
import cc.asylum.iridium.core.validation.annotation.Email;
import cc.asylum.iridium.core.validation.annotation.EscapeHtml;
import cc.asylum.iridium.core.validation.annotation.NotNull;
import cc.asylum.iridium.core.validation.annotation.Pattern;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ValidatorWriterTest {

  @Test
  void emptyTypeWritesNothing() throws IOException {
    final ValidatorWriter writer = new ValidatorWriter(mock(Types.class), mock(Elements.class), mock(Messager.class));
    final TypeElement type = mock(TypeElement.class);
    when(type.getKind()).thenAnswer(invocation -> ElementKind.CLASS);
    when(type.getEnclosedElements()).thenAnswer(invocation -> List.of());
    assertFalse(writer.write(mock(Filer.class), "app.gen", type, "EmptyValidator"));
  }

  @Test
  void writesClassWithGettersAndFieldAccess() throws IOException {
    final Types types = mock(Types.class);
    final Elements elements = mock(Elements.class);
    final Filer filer = filer();
    final ValidatorWriter writer = new ValidatorWriter(types, elements, mock(Messager.class));
    final TypeElement type = type("app.Widget", ElementKind.CLASS);
    final VariableElement titled = field("title", Mirrors.declared("java.lang.String"), NotNull.class);
    when(titled.getAnnotation(NotNull.class)).thenAnswer(invocation -> notNull());
    when(titled.getAnnotation(Pattern.class)).thenAnswer(invocation -> pattern("a+"));
    when(titled.getAnnotation(Email.class)).thenAnswer(invocation -> email());
    final VariableElement flagged = field("active", Mirrors.primitive(TypeKind.BOOLEAN), NotNull.class);
    final VariableElement raw = field("note", Mirrors.declared("java.lang.String"), NotNull.class);
    final ExecutableElement getter = method("getTitle");
    final ExecutableElement flag = method("isActive");
    final ExecutableElement parameterized = method("getNote");
    when(parameterized.getParameters()).thenAnswer(invocation -> List.of(mock(VariableElement.class)));
    final Element nested = mock(Element.class);
    when(nested.getKind()).thenAnswer(invocation -> ElementKind.FIELD);
    when(type.getEnclosedElements()).thenAnswer(invocation -> List.of(nested, titled, getter, flagged, flag, raw, parameterized));
    assertTrue(writer.write(filer, "app.gen", type, "WidgetValidator"));
  }

  @Test
  void writesRecordWithEscape() throws IOException {
    final Filer filer = filer();
    final ValidatorWriter writer = new ValidatorWriter(mock(Types.class), mock(Elements.class), mock(Messager.class));
    final TypeElement type = type("app.Person", ElementKind.RECORD);
    final VariableElement name = field("name", Mirrors.declared("java.lang.String"), EscapeHtml.class);
    when(name.getAnnotation(EscapeHtml.class)).thenAnswer(invocation -> escape());
    final VariableElement age = field("age", Mirrors.primitive(TypeKind.INT), NotNull.class);
    when(type.getEnclosedElements()).thenAnswer(invocation -> List.of(name, age));
    final RecordComponentElement nameComponent = component("name");
    final RecordComponentElement ageComponent = component("age");
    when(type.getRecordComponents()).thenAnswer(invocation -> List.of(nameComponent, ageComponent));
    assertTrue(writer.write(filer, "app.gen", type, "PersonValidator"));
  }

  private static Filer filer() throws IOException {
    final Filer filer = mock(Filer.class);
    final JavaFileObject file = mock(JavaFileObject.class);
    when(file.openWriter()).thenAnswer(invocation -> new StringWriter());
    when(filer.createSourceFile(anyString(), any(Element[].class))).thenAnswer(invocation -> file);
    return filer;
  }

  private static TypeElement type(final String qualified, final ElementKind kind) {
    final TypeElement type = mock(TypeElement.class);
    when(type.getKind()).thenAnswer(invocation -> kind);
    when(type.getQualifiedName()).thenAnswer(invocation -> Mirrors.name(qualified));
    when(type.getSimpleName()).thenAnswer(invocation -> Mirrors.name(qualified.substring(qualified.lastIndexOf('.') + 1)));
    final TypeMirror mirror = Mirrors.declared(qualified);
    when(type.asType()).thenAnswer(invocation -> mirror);
    return type;
  }

  private static VariableElement field(final String name, final TypeMirror type, final Class<? extends java.lang.annotation.Annotation> constraint) {
    final VariableElement field = mock(VariableElement.class);
    when(field.getKind()).thenAnswer(invocation -> ElementKind.FIELD);
    when(field.getSimpleName()).thenAnswer(invocation -> Mirrors.name(name));
    when(field.asType()).thenAnswer(invocation -> type);
    when(field.getAnnotationMirrors()).thenAnswer(invocation -> List.of());
    Mirrors.constrain(field, constraint);
    return field;
  }

  private static ExecutableElement method(final String name) {
    final ExecutableElement method = mock(ExecutableElement.class);
    when(method.getKind()).thenAnswer(invocation -> ElementKind.METHOD);
    when(method.getSimpleName()).thenAnswer(invocation -> Mirrors.name(name));
    when(method.getParameters()).thenAnswer(invocation -> List.of());
    return method;
  }

  private static RecordComponentElement component(final String name) {
    final RecordComponentElement component = mock(RecordComponentElement.class);
    when(component.getSimpleName()).thenAnswer(invocation -> Mirrors.name(name));
    return component;
  }

  private static NotNull notNull() {
    return new NotNull() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return NotNull.class;
      }
    };
  }

  private static Pattern pattern(final String regexp) {
    return new Pattern() {
      @Override
      public String regexp() {
        return regexp;
      }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Pattern.class;
      }
    };
  }

  private static Email email() {
    return new Email() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Email.class;
      }
    };
  }

  private static EscapeHtml escape() {
    return new EscapeHtml() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return EscapeHtml.class;
      }
    };
  }
}
