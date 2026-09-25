package cc.asylum.iridium.reloading.watcher;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.result.Unit;
import cc.asylum.iridium.core.util.Names;
import cc.asylum.iridium.reloading.Reloader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

@Slf4j
@RequiredArgsConstructor
public class ClassWatcher implements Runnable {

  private final Path root;
  private final Reloader reloader;
  private final Map<String, Long> seen = new ConcurrentHashMap<>();

  @Override
  public void run() {
    Result.of(this::watch).ifErr(exception -> {
      if (exception instanceof InterruptedException) {
        Thread.currentThread().interrupt();
        return;
      }

      log.error("class watcher failed", exception);
    });
  }

  private Unit watch() throws Exception {
    try (final var watchService = FileSystems.getDefault().newWatchService()) {
      registerTree(root, watchService);

      while (true) {
        final var key = watchService.take();
        handle(key, watchService);

        if (!key.reset()) {
          break;
        }
      }
    }

    return Unit.INSTANCE;
  }

  private void handle(final WatchKey key, final WatchService watchService) {
    final var directory = (Path) key.watchable();

    for (final WatchEvent<?> event : key.pollEvents()) {
      final var file = directory.resolve((Path) event.context());

      if (Files.isDirectory(file) && event.kind() == ENTRY_CREATE) {
        Result.of(() -> {
          registerTree(file, watchService);
          return Unit.INSTANCE;
        }).ifErr(exception -> log.error("failed to watch {}", file, exception));

        continue;
      }

      if (file.toString().endsWith(".class") && event.kind() != ENTRY_DELETE) {
        tryReload(file);
      }
    }
  }

  private void tryReload(final Path classFile) {
    Result.of(() -> changed(classFile))
        .ifOk(name -> name.ifPresent(binaryName -> reload(classFile, binaryName)))
        .ifErr(exception -> {
          if (exception instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return;
          }

          log.error("failed to inspect {}", classFile.getFileName(), exception);
        });
  }

  private Optional<String> changed(final Path classFile) throws Exception {
    Thread.sleep(50);

    final var name = binaryName(classFile);
    final var modified = Files.getLastModifiedTime(classFile).toMillis();
    final var previous = seen.put(name, modified);

    if (previous != null && previous == modified) {
      return Optional.empty();
    }

    return Optional.of(name);
  }

  private void reload(final Path classFile, final String binaryName) {
    reloader.reload(classFile, binaryName).ifErr(exception -> {
      if (exception instanceof ClassNotFoundException) {
        return;
      }

      log.error("failed to reload {}", classFile.getFileName(), exception);
    });
  }

  private String binaryName(final Path classFile) {
    final var relative = root.relativize(classFile).toString();

    return Names.binary(relative);
  }

  private void registerTree(final Path start, final WatchService watchService) throws Exception {
    try (final var walk = Files.walk(start)) {
      for (final var directory : walk.filter(Files::isDirectory).toList()) {
        directory.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
      }
    }
  }
}
