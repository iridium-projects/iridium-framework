package cc.asylum.iridium.web.processor.client;

import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.web.http.HttpClient;
import cc.asylum.iridium.web.http.RequestMapping;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class HttpClientModelGenerateTest {

  @Test
  void generatesSkipsAndRejects() throws Exception {
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

    final TypeElement emptyUrl = client("Empty", "", List.of());
    final TypeElement skipped = client("Skipped", "http://host", List.of(field(), defaultMethod(), staticMethod(), unmapped()));
    final TypeElement blankPath = client("Blank", "http://host", List.of(mapped("missing", "")));
    final TypeElement written = client("Users", "http://host", List.of(mapped("list", "/users")));

    final RoundEnvironment round = mock(RoundEnvironment.class);
    when(round.getElementsAnnotatedWith(HttpClient.class)).thenAnswer(invocation -> Set.<Element>of(emptyUrl, skipped, blankPath, written));
    packageOf(elements, written);
    HttpClientModel.generate(new Processing(types, elements, messager, filer, round));

    when(round.getElementsAnnotatedWith(HttpClient.class)).thenAnswer(invocation -> Set.<Element>of());
    HttpClientModel.generate(new Processing(types, elements, messager, filer, round));
  }

  private static TypeElement client(final String simple, final String url, final List<Element> enclosed) {
    final TypeElement client = mock(TypeElement.class);
    when(client.getKind()).thenAnswer(invocation -> ElementKind.INTERFACE);
    when(client.getSimpleName()).thenAnswer(invocation -> name(simple));
    when(client.getQualifiedName()).thenAnswer(invocation -> name("cc.asylum.demo." + simple));
    when(client.getEnclosedElements()).thenAnswer(invocation -> enclosed);
    final AnnotationMirror http = annotation(HttpClient.class.getCanonicalName(), Map.of("url", url));
    when(client.getAnnotationMirrors()).thenAnswer(invocation -> List.of(http));
    return client;
  }

  private static Element field() {
    final Element field = mock(Element.class);
    when(field.getKind()).thenAnswer(invocation -> ElementKind.FIELD);
    return field;
  }

  private static ExecutableElement defaultMethod() {
    final ExecutableElement method = method("def");
    when(method.isDefault()).thenAnswer(invocation -> true);
    return method;
  }

  private static ExecutableElement staticMethod() {
    final ExecutableElement method = method("stat");
    when(method.getModifiers()).thenAnswer(invocation -> Set.of(Modifier.STATIC));
    return method;
  }

  private static ExecutableElement unmapped() {
    return method("raw");
  }

  private static ExecutableElement mapped(final String simple, final String path) {
    final ExecutableElement method = method(simple);
    when(method.getReturnType()).thenAnswer(invocation -> {
      final TypeMirror type = mock(TypeMirror.class);
      when(type.getKind()).thenAnswer(call -> TypeKind.VOID);
      when(type.toString()).thenAnswer(call -> "void");
      return type;
    });
    when(method.getParameters()).thenAnswer(invocation -> List.of());
    when(method.getThrownTypes()).thenAnswer(invocation -> List.of());
    final AnnotationMirror mapping = annotation(RequestMapping.class.getCanonicalName(), Map.of("value", path));
    when(method.getAnnotationMirrors()).thenAnswer(invocation -> List.of(mapping));
    return method;
  }

  private static ExecutableElement method(final String simple) {
    final ExecutableElement method = mock(ExecutableElement.class);
    when(method.getKind()).thenAnswer(invocation -> ElementKind.METHOD);
    when(method.getSimpleName()).thenAnswer(invocation -> name(simple));
    when(method.getAnnotationMirrors()).thenAnswer(invocation -> List.of());
    when(method.getModifiers()).thenAnswer(invocation -> Set.of());
    when(method.isDefault()).thenAnswer(invocation -> false);
    return method;
  }

  private static void packageOf(final Elements elements, final TypeElement client) {
    final PackageElement pkg = mock(PackageElement.class);
    when(pkg.getQualifiedName()).thenAnswer(invocation -> name("cc.asylum.demo"));
    when(elements.getPackageOf(client)).thenAnswer(invocation -> pkg);
  }

  private static AnnotationMirror annotation(final String qualified, final Map<String, Object> members) {
    final AnnotationMirror mirror = mock(AnnotationMirror.class);
    final DeclaredType type = mock(DeclaredType.class);
    final TypeElement element = mock(TypeElement.class);
    when(element.getQualifiedName()).thenAnswer(invocation -> name(qualified));
    when(type.asElement()).thenAnswer(invocation -> element);
    when(mirror.getAnnotationType()).thenAnswer(invocation -> type);
    final Map<ExecutableElement, AnnotationValue> values = new LinkedHashMap<>();
    for (final Map.Entry<String, Object> entry : members.entrySet()) {
      final ExecutableElement key = mock(ExecutableElement.class);
      when(key.getSimpleName()).thenAnswer(invocation -> name(entry.getKey()));
      final AnnotationValue value = mock(AnnotationValue.class);
      when(value.getValue()).thenAnswer(invocation -> entry.getValue());
      values.put(key, value);
    }
    when(mirror.getElementValues()).thenAnswer(invocation -> values);
    return mirror;
  }

  private static Name name(final String text) {
    final Name name = mock(Name.class);
    when(name.toString()).thenAnswer(invocation -> text);
    when(name.contentEquals(text)).thenAnswer(invocation -> true);
    return name;
  }
}
