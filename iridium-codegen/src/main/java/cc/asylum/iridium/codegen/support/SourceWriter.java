package cc.asylum.iridium.codegen.support;

import com.io7m.jodist.AnnotationSpec;
import com.io7m.jodist.ClassName;
import com.io7m.jodist.JavaFile;
import com.io7m.jodist.TypeSpec;
import cc.asylum.iridium.core.annotation.Generated;
import cc.asylum.iridium.core.annotation.Internal;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;

@Internal
public final class SourceWriter {

  private static final String AUTHOR = "Iridium";

  private SourceWriter() {
  }

  public static TypeSpec.Builder generatedType(final String name) {
    return TypeSpec.classBuilder(name)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addAnnotation(generatedAnnotation());
  }

  private static AnnotationSpec generatedAnnotation() {
    return AnnotationSpec.builder(ClassName.get(Generated.class))
        .addMember("author", "$S", AUTHOR)
        .addMember("date", "$S", Instant.now().toString())
        .build();
  }

  public static void writeJava(
      final Filer filer,
      final String pkg,
      final TypeSpec spec,
      final Element... originatingElements
  ) {
    final String fqcn = pkg + "." + spec.name;
    try {
      final JavaFileObject source = filer.createSourceFile(fqcn, originatingElements);
      try (final Writer writer = source.openWriter()) {
        JavaFile.builder(pkg, spec).build().writeTo(writer);
      }
    } catch (final IOException e) {
      throw new RuntimeException("Failed to generate " + fqcn, e);
    }
  }

  public static void writeService(
      final Filer filer,
      final Class<?> service,
      final String implFqcn,
      final Element... originatingElements
  ) {
    writeService(filer, service.getName(), implFqcn, originatingElements);
  }

  public static void writeService(
      final Filer filer,
      final String serviceName,
      final String implFqcn,
      final Element... originatingElements
  ) {
    try {
      final var file = filer.createResource(
          StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + serviceName, originatingElements);
      try (final Writer writer = file.openWriter()) {
        writer.write(implFqcn);
        writer.write(System.lineSeparator());
      }
    } catch (final IOException e) {
      throw new RuntimeException("Failed to write service file for " + serviceName, e);
    }
  }
}
