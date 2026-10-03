package cc.asylum.iridium.core.processor.bean;

import cc.asylum.iridium.codegen.Register;
import cc.asylum.iridium.core.processor.Mirrors;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class BeanProcessorTest {

  @Test
  void registersConstructedAndSuffixedBeans() throws Exception {
    final Elements elements = mock(Elements.class);
    final BeanProcessor processor = new BeanProcessor();
    processor.init(environment(elements));
    assertEquals(Set.of("*"), processor.getSupportedAnnotationTypes());
    assertEquals(SourceVersion.latestSupported(), processor.getSupportedSourceVersion());

    final Element packageRoot = mock(Element.class);
    when(packageRoot.getKind()).thenAnswer(invocation -> ElementKind.PACKAGE);
    final TypeElement plain = type("Plain", "app.Plain", null);
    final TypeElement widget = type("Widget", "app.Widget", "");
    final TypeElement again = type("Widget", "app.Again", "");
    final TypeElement suffixed = type("View", "app.View", "Impl");
    pack(elements, plain, "app");
    pack(elements, widget, "app");
    pack(elements, again, "app");
    pack(elements, suffixed, "app");
    final ExecutableElement constructor = mock(ExecutableElement.class);
    when(constructor.getKind()).thenAnswer(invocation -> ElementKind.CONSTRUCTOR);
    when(constructor.getParameters()).thenAnswer(invocation -> List.of());
    when(widget.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor));
    when(again.getEnclosedElements()).thenAnswer(invocation -> List.of());
    when(suffixed.getEnclosedElements()).thenAnswer(invocation -> List.of());

    final RoundEnvironment round = mock(RoundEnvironment.class);
    when(round.processingOver()).thenAnswer(invocation -> false);
    when(round.getRootElements()).thenAnswer(invocation -> Set.<Element>of(packageRoot, plain, widget, again, suffixed));
    when(round.getElementsAnnotatedWith(any(Class.class))).thenAnswer(invocation -> Set.of());
    assertFalse(processor.process(Set.of(), round));

    when(round.getRootElements()).thenAnswer(invocation -> Set.of());
    assertFalse(processor.process(Set.of(), round));
    when(round.processingOver()).thenAnswer(invocation -> true);
    assertFalse(processor.process(Set.of(), round));
  }

  private static ProcessingEnvironment environment(final Elements elements) throws Exception {
    final ProcessingEnvironment environment = mock(ProcessingEnvironment.class);
    final Filer filer = mock(Filer.class);
    final JavaFileObject file = mock(JavaFileObject.class);
    when(file.openWriter()).thenAnswer(invocation -> new StringWriter());
    when(filer.createSourceFile(anyString(), any(Element[].class))).thenAnswer(invocation -> file);
    final FileObject resource = mock(FileObject.class);
    when(resource.openWriter()).thenAnswer(invocation -> new StringWriter());
    when(filer.createResource(any(StandardLocation.class), anyString(), anyString(), any(Element[].class))).thenAnswer(invocation -> resource);
    when(environment.getElementUtils()).thenAnswer(invocation -> elements);
    when(environment.getTypeUtils()).thenAnswer(invocation -> mock(Types.class));
    when(environment.getFiler()).thenAnswer(invocation -> filer);
    when(environment.getMessager()).thenAnswer(invocation -> mock(Messager.class));
    when(environment.getLocale()).thenAnswer(invocation -> Locale.ROOT);
    when(environment.getOptions()).thenAnswer(invocation -> Map.of());
    when(environment.getSourceVersion()).thenAnswer(invocation -> SourceVersion.latestSupported());
    return environment;
  }

  private static void pack(final Elements elements, final Element element, final String qualified) {
    final PackageElement pkg = mock(PackageElement.class);
    when(pkg.getQualifiedName()).thenAnswer(invocation -> Mirrors.name(qualified));
    when(elements.getPackageOf(element)).thenAnswer(invocation -> pkg);
  }

  private static TypeElement type(final String simple, final String qualified, final String suffix) {
    final TypeElement type = mock(TypeElement.class);
    when(type.getKind()).thenAnswer(invocation -> ElementKind.CLASS);
    when(type.getSimpleName()).thenAnswer(invocation -> Mirrors.name(simple));
    final Name qualifiedName = Mirrors.name(qualified);
    when(type.getQualifiedName()).thenAnswer(invocation -> qualifiedName);
    when(type.asType()).thenAnswer(invocation -> Mirrors.declared(qualified));
    when(type.getEnclosedElements()).thenAnswer(invocation -> List.of());
    if (suffix == null) {
      when(type.getAnnotationMirrors()).thenAnswer(invocation -> List.of());
      return type;
    }
    final AnnotationMirror mirror = mock(AnnotationMirror.class);
    final DeclaredType annotationType = mock(DeclaredType.class);
    final TypeElement annotation = mock(TypeElement.class);
    when(annotation.getAnnotation(Register.class)).thenAnswer(invocation -> register(suffix));
    when(annotationType.asElement()).thenAnswer(invocation -> annotation);
    when(mirror.getAnnotationType()).thenAnswer(invocation -> annotationType);
    when(type.getAnnotationMirrors()).thenAnswer(invocation -> List.of(mirror));
    return type;
  }

  private static Register register(final String suffix) {
    return new Register() {
      @Override
      public String suffix() {
        return suffix;
      }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Register.class;
      }
    };
  }
}
