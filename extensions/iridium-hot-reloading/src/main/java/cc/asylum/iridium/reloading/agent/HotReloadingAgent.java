package cc.asylum.iridium.reloading.agent;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;

import cc.asylum.iridium.reloading.Reloader;
import cc.asylum.iridium.reloading.watcher.ClassWatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public final class HotReloadingAgent {

  private final Instrumentation instrumentation;

  public static void premain(final String args, final Instrumentation instrumentation) {
    new HotReloadingAgent(instrumentation).start(args);
  }

  public static void agentmain(final String args, final Instrumentation instrumentation) {
    new HotReloadingAgent(instrumentation).start(args);
  }

  void start(final String args) {
    final var watchDir = Path.of(args == null || args.isBlank() ? "build/classes/java/main" : args)
        .toAbsolutePath()
        .normalize();

    Thread.ofPlatform()
        .name("hot-reload-watcher")
        .daemon()
        .start(new ClassWatcher(watchDir, new Reloader(instrumentation)));

    log.info("watching {}", watchDir);
  }
}
