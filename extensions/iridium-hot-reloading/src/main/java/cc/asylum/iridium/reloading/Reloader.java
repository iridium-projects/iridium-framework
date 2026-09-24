package cc.asylum.iridium.reloading;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;

import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.result.Unit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class Reloader {

  private final Instrumentation instrumentation;

  public Result<Unit, Exception> reload(final Path classFile, final String binaryName) {
    return Result.of(() -> {
      final var clazz = Class.forName(binaryName);
      final var bytes = Files.readAllBytes(classFile);

      instrumentation.redefineClasses(new ClassDefinition(clazz, bytes));

      log.info("reloading class " + clazz.getSimpleName());

      return Unit.INSTANCE;
    });
  }
}
