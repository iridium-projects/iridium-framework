package cc.asylum.iridium.core.processor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class Mirrors {

  private Mirrors() {
  }

  public static Name name(final String value) {
    final Name name = mock(Name.class);
    when(name.toString()).thenAnswer(invocation -> value);
    when(name.contentEquals(value)).thenAnswer(invocation -> true);
    when(name.contentEquals(org.mockito.ArgumentMatchers.any(CharSequence.class)))
      .thenAnswer(invocation -> value.contentEquals(invocation.getArgument(0, CharSequence.class)));
    return name;
  }

  public static TypeMirror declared(final String qualified) {
    final DeclaredType type = mock(DeclaredType.class);
    final TypeElement element = mock(TypeElement.class);
    when(type.getKind()).thenAnswer(invocation -> TypeKind.DECLARED);
    when(type.toString()).thenAnswer(invocation -> qualified);
    when(element.getQualifiedName()).thenAnswer(invocation -> name(qualified));
    when(type.asElement()).thenAnswer(invocation -> element);
    when(type.getTypeArguments()).thenAnswer(invocation -> List.of());
    return type;
  }

  public static TypeMirror primitive(final TypeKind kind) {
    final TypeMirror type = mock(TypeMirror.class);
    when(type.getKind()).thenAnswer(invocation -> kind);
    when(type.toString()).thenAnswer(invocation -> kind.name().toLowerCase());
    return type;
  }

  public static TypeMirror array(final TypeMirror component) {
    final javax.lang.model.type.ArrayType type = mock(javax.lang.model.type.ArrayType.class);
    when(type.getKind()).thenAnswer(invocation -> TypeKind.ARRAY);
    when(type.getComponentType()).thenAnswer(invocation -> component);
    when(type.toString()).thenAnswer(invocation -> component + "[]");
    return type;
  }

  static void pack(final javax.lang.model.util.Elements elements, final Element element, final String qualified) {
    final PackageElement pkg = mock(PackageElement.class);
    when(pkg.getQualifiedName()).thenAnswer(invocation -> name(qualified));
    when(elements.getPackageOf(element)).thenAnswer(invocation -> pkg);
  }

  public static void constrain(final Element element, final Class<? extends Annotation> annotation) {
    final List<AnnotationMirror> mirrors = new ArrayList<>(element.getAnnotationMirrors());
    mirrors.add(constraint(annotation));
    when(element.getAnnotationMirrors()).thenAnswer(invocation -> mirrors);
  }

  private static AnnotationMirror constraint(final Class<? extends Annotation> annotation) {
    final AnnotationMirror mirror = mock(AnnotationMirror.class);
    final DeclaredType type = mock(DeclaredType.class);
    final TypeElement owner = mock(TypeElement.class);
    when(owner.getQualifiedName()).thenAnswer(invocation -> name(annotation.getCanonicalName()));
    when(owner.getAnnotation(cc.asylum.iridium.core.validation.annotation.Constraint.class)).thenAnswer(invocation -> constraintMarker());
    when(type.asElement()).thenAnswer(invocation -> owner);
    when(mirror.getAnnotationType()).thenAnswer(invocation -> type);
    return mirror;
  }

  private static cc.asylum.iridium.core.validation.annotation.Constraint constraintMarker() {
    return new cc.asylum.iridium.core.validation.annotation.Constraint() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return cc.asylum.iridium.core.validation.annotation.Constraint.class;
      }
    };
  }
}
