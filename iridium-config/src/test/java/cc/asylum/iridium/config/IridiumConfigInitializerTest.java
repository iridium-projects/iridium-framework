package cc.asylum.iridium.config;

import cc.asylum.iridium.core.config.ConfigInitializer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IridiumConfigInitializerTest {

  @Test
  void prepareForwardsArgsToConfig() throws Exception {
    try (URLClassLoader loader = isolated()) {
      final Class<?> type = loader.loadClass(IridiumConfigInitializer.class.getName());
      final Object initializer = type.getDeclaredConstructor().newInstance();
      assertTrue(loader.loadClass(ConfigInitializer.class.getName()).isInstance(initializer));

      final Method prepare = type.getMethod("prepare", String[].class);
      final Thread thread = Thread.currentThread();
      final ClassLoader previous = thread.getContextClassLoader();
      thread.setContextClassLoader(loader);
      try {
        prepare.invoke(initializer, (Object) new String[] { "--app.name=from-args" });
      } finally {
        thread.setContextClassLoader(previous);
      }

      final Class<?> config = loader.loadClass(Config.class.getName());
      final Object found = config.getMethod("string", String.class).invoke(null, "app.name");
      assertTrue((Boolean) found.getClass().getMethod("isOk").invoke(found));
      assertEquals("from-args", found.getClass().getMethod("unwrap").invoke(found));
    }
  }

  @Test
  void prepareAcceptsNullArgs() throws Exception {
    try (URLClassLoader loader = isolated()) {
      final Class<?> type = loader.loadClass(IridiumConfigInitializer.class.getName());
      final Object initializer = type.getDeclaredConstructor().newInstance();
      final Thread thread = Thread.currentThread();
      final ClassLoader previous = thread.getContextClassLoader();
      thread.setContextClassLoader(loader);
      try {
        type.getMethod("prepare", String[].class).invoke(initializer, new Object[] { null });
        loader.loadClass(Config.class.getName()).getMethod("has", String.class).invoke(null, "warmup");
      } finally {
        thread.setContextClassLoader(previous);
      }

      final Class<?> config = loader.loadClass(Config.class.getName());
      final Object found = config.getMethod("string", String.class).invoke(null, "missing.key");
      assertEquals(Boolean.FALSE, found.getClass().getMethod("isOk").invoke(found));
    }
  }

  private static URLClassLoader isolated() throws Exception {
    final Path temp = Files.createTempDirectory("iridium-config");
    return new URLClassLoader(new URL[] {
        temp.toUri().toURL(),
        IridiumConfigInitializer.class.getProtectionDomain().getCodeSource().getLocation(),
        cc.asylum.iridium.core.result.Result.class.getProtectionDomain().getCodeSource().getLocation(),
        org.yaml.snakeyaml.Yaml.class.getProtectionDomain().getCodeSource().getLocation()
    }, ClassLoader.getPlatformClassLoader());
  }
}
