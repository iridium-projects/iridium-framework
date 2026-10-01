package cc.asylum.iridium.codegen;

import org.junit.jupiter.api.Test;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class ServiceProcessorTest {

  @Test
  void supportedTypes() {
    assertEquals(Set.of(Service.class.getCanonicalName()), new ServiceProcessor().getSupportedAnnotationTypes());
  }

  @Test
  void skipsNonClassesNullAnnotationsAndFrameworkTypes() {
    final RoundEnvironment round = mock(RoundEnvironment.class);
    final Element field = mock(Element.class);
    when(field.getKind()).thenAnswer(invocation -> ElementKind.FIELD);
    final TypeElement missing = type("app.Missing", ElementKind.CLASS, List.of(), objectType(), null);
    final TypeElement framework = type("cc.asylum.iridium.codegen.Internal", ElementKind.CLASS, List.of(), objectType(), sample(false));
    when(round.getElementsAnnotatedWith(Service.class)).thenAnswer(invocation -> java.util.Set.<Element>of(field, missing, framework));
    final ServiceProcessor processor = new ServiceProcessor();
    processor.process(processing(round, mock(Filer.class)));
    processor.finish(processing(round, mock(Filer.class)));
  }

  @Test
  void writesDeclaredContractsAndIncremental() throws Exception {
    final TypeElement contract = type("app.Api", ElementKind.INTERFACE, List.of(), objectType(), null);
    final DeclaredType contractType = declared(contract);
    final AnnotationValue item = mock(AnnotationValue.class);
    when(item.getValue()).thenAnswer(invocation -> contractType);
    final TypeElement type = type("app.Impl", ElementKind.CLASS, List.of(), processorType(), sample(true));
    when(type.getAnnotationMirrors()).thenAnswer(invocation -> java.util.List.<AnnotationMirror>of(serviceMirror(List.of(item))));
    final RoundEnvironment round = mock(RoundEnvironment.class);
    when(round.getElementsAnnotatedWith(Service.class)).thenAnswer(invocation -> java.util.Set.<Element>of(type));
    final Filer filer = mock(Filer.class);
    final StringWriter services = writer(filer, "META-INF/services/app.Api");
    final StringWriter incremental = writer(filer, "META-INF/gradle/incremental.annotation.processors");
    final ServiceProcessor processor = new ServiceProcessor();
    final Processing processing = processing(round, filer);
    processor.process(processing);
    processor.finish(processing);
    assertEquals("app.Impl\n", services.toString());
    assertTrue(incremental.toString().contains("app.Impl,ISOLATING"));
  }

  @Test
  void collectsInterfacesAndWalksSuperclass() throws Exception {
    final TypeElement iface = type("app.Api", ElementKind.INTERFACE, List.of(), objectType(), null);
    final TypeElement parent = type("app.Base", ElementKind.CLASS, List.of(declared(iface)), abstractProcessorType(), null);
    final TypeElement type = type("app.Impl", ElementKind.CLASS, List.of(declared(type("app.Extra", ElementKind.INTERFACE, List.of(), objectType(), null))), declared(parent), sample(false));
    final RoundEnvironment round = mock(RoundEnvironment.class);
    when(round.getElementsAnnotatedWith(Service.class)).thenAnswer(invocation -> java.util.Set.<Element>of(type));
    final Filer filer = mock(Filer.class);
    final StringWriter extra = writer(filer, "META-INF/services/app.Extra");
    final StringWriter api = writer(filer, "META-INF/services/app.Api");
    final StringWriter processors = writer(filer, "META-INF/services/javax.annotation.processing.Processor");
    final StringWriter incremental = writer(filer, "META-INF/gradle/incremental.annotation.processors");
    final ServiceProcessor processor = new ServiceProcessor();
    final Processing processing = processing(round, filer);
    processor.process(processing);
    processor.finish(processing);
    assertEquals("app.Impl\n", extra.toString());
    assertEquals("app.Impl\n", api.toString());
    assertEquals("app.Impl\n", processors.toString());
    assertTrue(incremental.toString().contains("AGGREGATING"));
  }

  @Test
  void ignoresUnusableContractValuesAndStopsOnBrokenSuperclass() throws Exception {
    final AnnotationValue raw = mock(AnnotationValue.class);
    when(raw.getValue()).thenAnswer(invocation -> "nope");
    final AnnotationValue mirror = mock(AnnotationValue.class);
    final TypeMirror notDeclared = mock(TypeMirror.class);
    when(notDeclared.getKind()).thenAnswer(invocation -> TypeKind.INT);
    when(mirror.getValue()).thenAnswer(invocation -> notDeclared);
    final TypeElement type = type("app.Broken", ElementKind.CLASS, List.of(notDeclared), notDeclared, sample(false));
    when(type.getAnnotationMirrors()).thenAnswer(invocation -> java.util.List.<AnnotationMirror>of(serviceMirror(List.of(raw, mirror, "ignored"))));
    final RoundEnvironment round = mock(RoundEnvironment.class);
    when(round.getElementsAnnotatedWith(Service.class)).thenAnswer(invocation -> java.util.Set.<Element>of(type));
    final Filer filer = mock(Filer.class);
    final ServiceProcessor processor = new ServiceProcessor();
    processor.process(processing(round, filer));
    processor.finish(processing(round, filer));
    verify(filer, never()).createResource(any(), any(), any());
  }

  @Test
  void writeFailure() throws Exception {
    final TypeElement iface = type("app.Api", ElementKind.INTERFACE, List.of(), objectType(), null);
    final TypeElement type = type("app.Impl", ElementKind.CLASS, List.of(declared(iface)), objectType(), sample(false));
    final RoundEnvironment round = mock(RoundEnvironment.class);
    when(round.getElementsAnnotatedWith(Service.class)).thenAnswer(invocation -> java.util.Set.<Element>of(type));
    final Filer filer = mock(Filer.class);
    when(filer.createResource(eq(StandardLocation.CLASS_OUTPUT), eq(""), eq("META-INF/services/app.Api"))).thenThrow(new IOException("nope"));
    final ServiceProcessor processor = new ServiceProcessor();
    final Processing processing = processing(round, filer);
    processor.process(processing);
    final RuntimeException failure = assertThrows(RuntimeException.class, () -> processor.finish(processing));
    assertEquals("Failed to write META-INF/services/app.Api", failure.getMessage());
  }

  @Test
  void stopsWhenSuperclassChainEnds() {
    final TypeElement parent = type("app.Base", ElementKind.CLASS, List.of(), mock(TypeMirror.class), null);
    when(parent.getSuperclass()).thenAnswer(invocation -> null);
    final TypeElement type = type("app.Impl", ElementKind.CLASS, List.of(), declared(parent), sample(false));
    final RoundEnvironment round = mock(RoundEnvironment.class);
    when(round.getElementsAnnotatedWith(Service.class)).thenAnswer(invocation -> java.util.Set.<Element>of(type));
    new ServiceProcessor().process(processing(round, mock(Filer.class)));
  }

  @Test
  void skipsFrameworkProcessorAndObjectSuperclass() {
    final TypeElement framework = type("cc.asylum.iridium.codegen.ServiceProcessor", ElementKind.CLASS, List.of(), declared(type(Processor.class.getCanonicalName(), ElementKind.CLASS, List.of(), objectType(), null)), sample(false));
    final TypeElement plain = type("app.Plain", ElementKind.CLASS, List.of(), objectType(), sample(false));
    final RoundEnvironment round = mock(RoundEnvironment.class);
    when(round.getElementsAnnotatedWith(Service.class)).thenAnswer(invocation -> java.util.Set.<Element>of(framework, plain));
    final Filer filer = mock(Filer.class);
    final ServiceProcessor processor = new ServiceProcessor();
    processor.process(processing(round, filer));
    processor.finish(processing(round, filer));
  }

  private static Processing processing(final RoundEnvironment round, final Filer filer) {
    return new Processing(mock(Types.class), mock(Elements.class), mock(Messager.class), filer, round);
  }

  private static StringWriter writer(final Filer filer, final String path) throws Exception {
    final FileObject file = mock(FileObject.class);
    final StringWriter writer = new StringWriter();
    when(filer.createResource(StandardLocation.CLASS_OUTPUT, "", path)).thenAnswer(invocation -> file);
    when(file.openWriter()).thenAnswer(invocation -> writer);
    return writer;
  }

  private static Service sample(final boolean isolating) {
    return new Service() {
      @Override
      public Class<?>[] value() {
        return new Class<?>[0];
      }

      @Override
      public int order() {
        return 0;
      }

      @Override
      public boolean isolating() {
        return isolating;
      }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Service.class;
      }
    };
  }

  private static AnnotationMirror serviceMirror(final List<?> value) {
    final AnnotationMirror mirror = mock(AnnotationMirror.class);
    final DeclaredType type = mock(DeclaredType.class);
    final TypeElement element = type("cc.asylum.iridium.codegen.Service", ElementKind.ANNOTATION_TYPE, List.of(), objectType(), null);
    when(type.asElement()).thenAnswer(invocation -> element);
    when(mirror.getAnnotationType()).thenAnswer(invocation -> type);
    final ExecutableElement member = mock(ExecutableElement.class);
    final Name name = mock(Name.class);
    when(name.contentEquals("value")).thenAnswer(invocation -> true);
    when(member.getSimpleName()).thenAnswer(invocation -> name);
    final AnnotationValue annotationValue = mock(AnnotationValue.class);
    when(annotationValue.getValue()).thenAnswer(invocation -> value);
    when(mirror.getElementValues()).thenAnswer(invocation -> java.util.Map.of(member, annotationValue));
    return mirror;
  }

  private static TypeElement type(
    final String qualified,
    final ElementKind kind,
    final List<? extends TypeMirror> interfaces,
    final TypeMirror superclass,
    final Service service
  ) {
    final TypeElement element = mock(TypeElement.class);
    final Name name = mock(Name.class);
    when(name.toString()).thenAnswer(invocation -> qualified);
    when(name.contentEquals(qualified)).thenAnswer(invocation -> true);
    when(name.contentEquals("java.lang.Object")).thenAnswer(invocation -> "java.lang.Object".equals(qualified));
    when(name.contentEquals(Processor.class.getCanonicalName())).thenAnswer(invocation -> Processor.class.getCanonicalName().equals(qualified));
    when(element.getQualifiedName()).thenAnswer(invocation -> name);
    when(element.getKind()).thenAnswer(invocation -> kind);
    when(element.getInterfaces()).thenAnswer(invocation -> interfaces);
    when(element.getSuperclass()).thenAnswer(invocation -> superclass);
    when(element.getAnnotation(Service.class)).thenAnswer(invocation -> service);
    when(element.getAnnotationMirrors()).thenAnswer(invocation -> java.util.List.<AnnotationMirror>of());
    return element;
  }

  private static DeclaredType declared(final TypeElement element) {
    final DeclaredType type = mock(DeclaredType.class);
    when(type.getKind()).thenAnswer(invocation -> TypeKind.DECLARED);
    when(type.asElement()).thenAnswer(invocation -> element);
    return type;
  }

  private static TypeMirror objectType() {
    return declared(type("java.lang.Object", ElementKind.CLASS, List.of(), mock(TypeMirror.class), null));
  }

  private static TypeMirror processorType() {
    return declared(type(Processor.class.getCanonicalName(), ElementKind.CLASS, List.of(), objectType(), null));
  }

  private static TypeMirror abstractProcessorType() {
    return declared(type("javax.annotation.processing.AbstractProcessor", ElementKind.CLASS, List.of(), objectType(), null));
  }
}
