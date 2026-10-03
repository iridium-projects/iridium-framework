package cc.asylum.iridium.web.processor.route;

import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.core.component.Component;
import cc.asylum.iridium.web.controller.RestController;
import cc.asylum.iridium.web.middleware.Middleware;
import cc.asylum.iridium.web.processor.MirrorSupport;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class WebModelGenerateTest {

  @Test
  void generatesControllersAndMiddlewares() throws Exception {
    final Types types = mock(Types.class);
    final Elements elements = mock(Elements.class);
    final Messager messager = mock(Messager.class);
    final Filer filer = mock(Filer.class);
    final JavaFileObject file = mock(JavaFileObject.class);
    when(file.openWriter()).thenAnswer(invocation -> new StringWriter());
    when(filer.createSourceFile(anyString(), nullable(Element.class))).thenAnswer(invocation -> file);
    when(filer.createSourceFile(anyString(), any(Element[].class))).thenAnswer(invocation -> file);
    when(filer.createResource(any(), anyString(), anyString(), any(Element[].class))).thenAnswer(invocation -> {
      final FileObject resource = mock(FileObject.class);
      when(resource.openWriter()).thenAnswer(call -> new StringWriter());
      return resource;
    });

    final TypeElement controller = type("Users", ElementKind.CLASS);
    when(controller.getAnnotation(RestController.class)).thenAnswer(invocation -> rest(""));
    when(controller.getAnnotationMirrors()).thenAnswer(invocation -> List.of());
    when(controller.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(handler("list")));
    when(controller.asType()).thenAnswer(invocation -> typeMirror("cc.asylum.Users"));

    final TypeElement middleware = type("Auth", ElementKind.CLASS);
    when(middleware.getAnnotation(Component.class)).thenAnswer(invocation -> component());
    when(middleware.getAnnotation(RestController.class)).thenAnswer(invocation -> null);
    final TypeMirror middlewareType = typeMirror(Middleware.class.getCanonicalName());
    when(middleware.asType()).thenAnswer(invocation -> middlewareType);
    assignable(types, elements, middlewareType);
    when(middleware.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of(constructor()));

    final TypeElement broken = type("Broken", ElementKind.CLASS);
    when(broken.getAnnotation(Component.class)).thenAnswer(invocation -> component());
    when(broken.getAnnotation(RestController.class)).thenAnswer(invocation -> null);
    final TypeMirror brokenType = typeMirror("cc.asylum.Broken");
    when(broken.asType()).thenAnswer(invocation -> brokenType);
    when(types.isAssignable(brokenType, middlewareType)).thenAnswer(invocation -> true);
    when(broken.getEnclosedElements()).thenAnswer(invocation -> List.<Element>of());

    final TypeElement record = type("Row", ElementKind.RECORD);
    when(record.getAnnotation(RestController.class)).thenAnswer(invocation -> null);
    when(record.getAnnotation(Component.class)).thenAnswer(invocation -> null);

    final RoundEnvironment round = mock(RoundEnvironment.class);
    when(round.getRootElements()).thenAnswer(invocation -> Set.<Element>of(controller, middleware, broken, record));
    packageOf(elements, controller, middleware, broken, record);
    WebModel.generate(new Processing(types, elements, messager, filer, round));

    when(round.getRootElements()).thenAnswer(invocation -> Set.<Element>of());
    WebModel.generate(new Processing(types, elements, messager, filer, round));
  }

  private static void assignable(final Types types, final Elements elements, final TypeMirror type) {
    final TypeElement target = mock(TypeElement.class);
    when(elements.getTypeElement(Middleware.class.getCanonicalName())).thenAnswer(invocation -> target);
    when(target.asType()).thenAnswer(invocation -> type);
    when(types.isAssignable(type, type)).thenAnswer(invocation -> true);
  }

  private static void packageOf(final Elements elements, final TypeElement... types) {
    final PackageElement pkg = mock(PackageElement.class);
    when(pkg.getQualifiedName()).thenAnswer(invocation -> name("cc.asylum.demo"));
    for (final TypeElement type : types) {
      when(elements.getPackageOf(type)).thenAnswer(invocation -> pkg);
    }
  }

  private static ExecutableElement handler(final String simple) {
    final ExecutableElement method = mock(ExecutableElement.class);
    when(method.getKind()).thenAnswer(invocation -> ElementKind.METHOD);
    when(method.getSimpleName()).thenAnswer(invocation -> name(simple));
    when(method.getAnnotationMirrors()).thenAnswer(invocation -> List.of());
    when(method.getParameters()).thenAnswer(invocation -> List.of());
    return method;
  }

  private static ExecutableElement constructor() {
    final ExecutableElement constructor = mock(ExecutableElement.class);
    when(constructor.getKind()).thenAnswer(invocation -> ElementKind.CONSTRUCTOR);
    when(constructor.getParameters()).thenAnswer(invocation -> List.of());
    return constructor;
  }

  private static TypeElement type(final String simple, final ElementKind kind) {
    final TypeElement type = mock(TypeElement.class);
    when(type.getKind()).thenAnswer(invocation -> kind);
    when(type.getSimpleName()).thenAnswer(invocation -> name(simple));
    when(type.getQualifiedName()).thenAnswer(invocation -> name("cc.asylum." + simple));
    return type;
  }

  private static TypeMirror typeMirror(final String text) {
    return MirrorSupport.declared(text);
  }

  private static Name name(final String text) {
    return MirrorSupport.name(text);
  }

  private static RestController rest(final String value) {
    return new RestController() {
      @Override
      public String value() {
        return value;
      }

      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return RestController.class;
      }
    };
  }

  private static Component component() {
    return new Component() {
      @Override
      public Class<? extends java.lang.annotation.Annotation> annotationType() {
        return Component.class;
      }
    };
  }
}
