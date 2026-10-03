package cc.asylum.iridium.codegen;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import cc.asylum.iridium.codegen.model.Mirrors;

@Service(isolating = false)
public final class ServiceProcessor extends Processor {

  private static final String PROCESSOR = "javax.annotation.processing.Processor";
  private static final String INCREMENTAL = "META-INF/gradle/incremental.annotation.processors";

  private final Map<String, Set<String>> services = new LinkedHashMap<>();
  private final Map<String, Boolean> incremental = new LinkedHashMap<>();

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(Service.class.getCanonicalName());
  }

  @Override
  protected void process(final Processing processing) {
    for (final Element element : processing.round().getElementsAnnotatedWith(Service.class)) {
      if (element.getKind() != ElementKind.CLASS) {
        continue;
      }

      final TypeElement type = (TypeElement) element;
      final Service service = type.getAnnotation(Service.class);
      if (service == null) {
        continue;
      }

      final String fqcn = type.getQualifiedName().toString();
      if (fqcn.startsWith("cc.asylum.iridium.codegen.")) {
        continue;
      }

      for (final String contract : contracts(processing, type)) {
        services.computeIfAbsent(contract, ignored -> new LinkedHashSet<>()).add(fqcn);
      }

      if (isProcessor(processing, type)) {
        incremental.put(fqcn, service.isolating());
      }
    }
  }

  @Override
  protected void finish(final Processing processing) {
    if (services.isEmpty()) {
      return;
    }
    for (final Map.Entry<String, Set<String>> entry : services.entrySet()) {
      write(processing, "META-INF/services/" + entry.getKey(), String.join("\n", entry.getValue()) + "\n");
    }

    if (!incremental.isEmpty()) {
      final StringBuilder gradle = new StringBuilder();
      for (final Map.Entry<String, Boolean> entry : incremental.entrySet()) {
        gradle.append(entry.getKey())
            .append(',')
            .append(entry.getValue() ? "ISOLATING" : "AGGREGATING")
            .append('\n');
      }

      write(processing, INCREMENTAL, gradle.toString());
    }
  }

  private Set<String> contracts(final Processing processing, final TypeElement type) {
    final Set<String> declared = declaredContracts(type);
    if (!declared.isEmpty()) {
      return declared;
    }

    final Set<String> contracts = new LinkedHashSet<>();
    collect(processing, type, contracts);
    if (isProcessor(processing, type)) {
      contracts.add(PROCESSOR);
    }

    return contracts;
  }

  private Set<String> declaredContracts(final TypeElement type) {
    final Set<String> contracts = new LinkedHashSet<>();
    final AnnotationMirror mirror = Mirrors.of(type, Service.class);
    final Object value = Mirrors.memberValue(mirror, "value");
    if (value instanceof final java.util.List<?> list) {
      for (final Object item : list) {
        final Object unwrapped = item instanceof final AnnotationValue annotation ? annotation.getValue() : item;
        if (unwrapped instanceof final DeclaredType declared
            && declared.asElement() instanceof final TypeElement element) {
          contracts.add(element.getQualifiedName().toString());
        }
      }
    }
    return contracts;
  }

  private void collect(final Processing processing, final TypeElement type, final Set<String> contracts) {
    for (final TypeMirror iface : type.getInterfaces()) {
      if (iface instanceof final DeclaredType declared && declared.asElement() instanceof final TypeElement element) {
        contracts.add(element.getQualifiedName().toString());
      }
    }

    final TypeMirror superclass = type.getSuperclass();
    if (superclass instanceof final DeclaredType declared && declared.asElement() instanceof final TypeElement parent) {
      if (!parent.getQualifiedName().contentEquals("java.lang.Object")
          && !parent.getQualifiedName().contentEquals(Processor.class.getCanonicalName())) {
        collect(processing, parent, contracts);
      }
    }
  }

  private boolean isProcessor(final Processing processing, final TypeElement type) {
    TypeElement current = type;
    while (current != null) {
      final String name = current.getQualifiedName().toString();
      if (name.equals(Processor.class.getCanonicalName())
          || name.equals("javax.annotation.processing.AbstractProcessor")) {
        return true;
      }

      final TypeMirror superclass = current.getSuperclass();
      if (!(superclass instanceof final DeclaredType declared)
          || !(declared.asElement() instanceof final TypeElement parent)) {
        return false;
      }

      current = parent;
    }
    return false;
  }

  private void write(final Processing processing, final String path, final String content) {
    try {
      final var file = processing.filer().createResource(StandardLocation.CLASS_OUTPUT, "", path);
      try (final Writer writer = file.openWriter()) {
        writer.write(content);
      }
    } catch (final IOException e) {
      throw new RuntimeException("Failed to write " + path, e);
    }
  }
}
