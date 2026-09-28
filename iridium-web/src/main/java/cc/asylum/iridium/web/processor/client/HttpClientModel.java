package cc.asylum.iridium.web.processor.client;

import cc.asylum.iridium.codegen.model.Diagnostics;
import cc.asylum.iridium.codegen.model.Elements;
import cc.asylum.iridium.codegen.model.Mirrors;
import cc.asylum.iridium.codegen.Processing;
import cc.asylum.iridium.codegen.write.SourceWriter;
import cc.asylum.iridium.codegen.code.Types;
import cc.asylum.iridium.web.http.HttpClient;
import cc.asylum.iridium.web.http.RequestMapping;
import cc.asylum.iridium.web.processor.client.ClientMethod;
import cc.asylum.iridium.web.processor.client.GeneratedMethod;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

public final class HttpClientModel {

  private HttpClientModel() {
  }

  public static void generate(final Processing processing) {
    final ClientMethod writer = new ClientMethod(processing.types(), processing.elements(), processing.messager());
    for (final TypeElement client : Elements.annotatedTypes(processing.round(), HttpClient.class, ElementKind.INTERFACE)) {
      generate(client, writer, processing);
    }
  }

  private static void generate(
    final TypeElement client,
    final ClientMethod writer,
    final Processing processing
  ) {
    final AnnotationMirror mirror = Mirrors.of(client, HttpClient.class);
    final String url = Mirrors.string(mirror, "url", "");
    if (url.isEmpty()) {
      Diagnostics.error(processing.messager(), client, "@HttpClient url must not be empty");
      return;
    }

    final List<GeneratedMethod> methods = new ArrayList<>();
    for (final Element enclosed : client.getEnclosedElements()) {
      if (enclosed.getKind() != ElementKind.METHOD) {
        continue;
      }

      final ExecutableElement method = (ExecutableElement) enclosed;
      if (method.isDefault() || method.getModifiers().contains(Modifier.STATIC)) {
        continue;
      }

      final AnnotationMirror mapping = Mirrors.of(method, RequestMapping.class);
      if (mapping == null) {
        Diagnostics.error(processing.messager(), method, "@HttpClient methods require @RequestMapping");
        continue;
      }

      final GeneratedMethod generated = writer.emit(method, mapping, url);
      if (generated == null) {
        continue;
      }

      methods.add(generated);
    }

    if (methods.isEmpty()) {
      return;
    }

    SourceWriter.writeJava(
      processing.filer(),
      Elements.packageOf(processing.elements(), client),
      client.getSimpleName() + "Undertow",
      type -> {
        type.implements_(Types.of(client));
        for (final GeneratedMethod method : methods) {
          type.method(method.name(), method.configure());
        }
      },
      client);
  }
}
