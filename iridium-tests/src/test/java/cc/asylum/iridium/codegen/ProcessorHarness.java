package cc.asylum.iridium.codegen;

import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class ProcessorHarness {

  public record Result(boolean success, List<String> errors, Path generatedSources, Path classes) {
  }

  private ProcessorHarness() {
  }

  public static Result compile(final Map<String, String> sources, final Processor... processors)
      throws IOException {
    final Path work = Files.createTempDirectory("iridium-codegen-test");
    final Path classes = work.resolve("classes");
    final Path generated = work.resolve("generated");
    Files.createDirectories(classes);
    Files.createDirectories(generated);

    final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    try (final StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, null)) {
      final List<JavaFileObject> units = new ArrayList<>();
      for (final Map.Entry<String, String> source : sources.entrySet()) {
        units.add(new StringSource(source.getKey(), source.getValue()));
      }
      final List<String> options = List.of(
          "-classpath", System.getProperty("java.class.path"),
          "-d", classes.toString(),
          "-s", generated.toString(),
          "-parameters");
      final JavaCompiler.CompilationTask task = compiler.getTask(null, files, diagnostics, options, null, units);
      task.setProcessors(Arrays.asList(processors));
      final boolean success = task.call();
      final List<String> errors = diagnostics.getDiagnostics().stream()
          .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
          .map(diagnostic -> diagnostic.getSource() + ":" + diagnostic.getLineNumber()
              + " " + diagnostic.getMessage(null))
          .toList();
      return new Result(success, errors, generated, classes);
    }
  }

  public static String generatedSource(final Result result, final String... path) throws IOException {
    return Files.readString(result.generatedSources().resolve(Path.of("", path)));
  }

  public static URLClassLoader classLoader(final Result result) throws IOException {
    return new URLClassLoader(new URL[] {result.classes().toUri().toURL()}, ProcessorHarness.class.getClassLoader());
  }

  private static final class StringSource extends SimpleJavaFileObject {

    private final String code;

    StringSource(final String fqcn, final String code) {
      super(URI.create("string:///" + fqcn.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
      this.code = code;
    }

    @Override
    public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
      return code;
    }
  }
}
