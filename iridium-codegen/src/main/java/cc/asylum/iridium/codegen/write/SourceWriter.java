package cc.asylum.iridium.codegen.write;

import cc.asylum.forgery.Forgery;
import cc.asylum.forgery.SourceFile;
import cc.asylum.forgery.jodist.JodistBackend;
import cc.asylum.forgery.model.AnnotationUse;
import cc.asylum.forgery.model.TypeRef;
import cc.asylum.forgery.spi.JavaSourceBackend;
import cc.asylum.forgery.type.AnnotationUseBuilder;
import cc.asylum.forgery.type.ClassBuilder;
import cc.asylum.iridium.codegen.Generated;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.function.Consumer;

public final class SourceWriter {

  private static final String AUTHOR = "Iridium";
  private static final JavaSourceBackend BACKEND = new JodistBackend();

  private SourceWriter() {
  }

  public static void generatedClass(final ClassBuilder type, final Consumer<ClassBuilder> configure) {
    type.public_().final_();
    type.annotation(generated());
    configure.accept(type);
  }

  public static NestedTypes nested(final ClassBuilder owner) {
    return (name, configure) -> {
      owner.nestedClass(name, nested -> {
        nested.private_().static_().final_();
        nested.annotation(generated());
        configure.accept(nested);
      });
      return TypeRef.of(name);
    };
  }

  private static AnnotationUse generated() {
    return new AnnotationUseBuilder(TypeRef.of(Generated.class))
      .member("author", AUTHOR)
      .member("date", Instant.now().toString())
      .build();
  }

  public static void writeJava(
    final Filer filer,
    final String pkg,
    final String name,
    final Consumer<ClassBuilder> configure,
    final Element... originatingElements
  ) {
    final SourceFile file = Forgery.file(pkg, source -> source.class_(name, type -> generatedClass(type, configure)));
    try {
      BACKEND.write(file, filer);
    } catch (final IOException e) {
      throw new RuntimeException("Failed to generate " + pkg + "." + name, e);
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
      final var file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + serviceName, originatingElements);
      try (final Writer writer = file.openWriter()) {
        writer.write(implFqcn);
        writer.write(System.lineSeparator());
      }
    } catch (final IOException e) {
      throw new RuntimeException("Failed to write service file for " + serviceName, e);
    }
  }
}
