package cc.asylum.iridium.reloading;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.Files;
import java.nio.file.Path;

import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.result.Unit;
import cc.asylum.iridium.core.util.Strings;
import cc.asylum.iridium.web.webserver.WebServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class Reloader {

  private final Instrumentation instrumentation;

  public Result<Unit, Exception> reload(final Path classFile, final String binaryName) {
    return Result.of(() -> {
      final Class<?> clazz = Class.forName(binaryName);
      redefine(classFile, clazz);

      if (generated(binaryName)) {
        rebind();
      }

      return Unit.INSTANCE;
    });
  }

  private void redefine(final Path classFile, final Class<?> clazz) throws Exception {
    final var bytes = Files.readAllBytes(classFile);

    try {
      instrumentation.redefineClasses(new ClassDefinition(clazz, bytes));
      log.info("reloading class {}", clazz.getName());
    } catch (final UnsupportedOperationException | UnmodifiableClassException exception) {
      log.warn("skipped redefine of {}: {}", clazz.getSimpleName(), exception.toString());
    }
  }

  private void rebind() {
    final Result<Unit, Exception> rebound = WebServer.reload();
    if (rebound.isErr()) {
      throw new IllegalStateException("failed to rebind generated wiring", rebound.unwrapErr());
    }

    log.info("rebound generated wiring");
  }

  static boolean generated(final String binaryName) {
    final var simpleName = Strings.simpleName(binaryName);

    return simpleName.endsWith("RegistrarGenerated") || simpleName.endsWith("Validator");
  }
}
