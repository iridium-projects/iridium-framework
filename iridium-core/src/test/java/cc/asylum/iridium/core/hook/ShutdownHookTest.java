package cc.asylum.iridium.core.hook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
final class ShutdownHookTest {

  @Mock
  private ShutdownHook hook;

  @Test
  void defaultPriorityIsZero() {
    hook.run();
    verify(hook).run();
    assertEquals(0, hook.priority());
    assertEquals(0, new ShutdownHook() {
      @Override
      public void run() {
      }
    }.priority());
  }

  @Test
  void onShutdownIsRetainedInClassFiles() throws Exception {
    assertTrue(OnShutdown.class.isAnnotation());
    assertEquals(0, OnShutdown.class.getMethod("priority").getDefaultValue());
    try (InputStream stream = Sample.class.getResourceAsStream("ShutdownHookTest$Sample.class")) {
      final byte[] bytes = stream.readAllBytes();
      final String utf8 = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
      assertTrue(utf8.contains("Lcc/asylum/iridium/core/hook/OnShutdown;"));
      assertTrue(utf8.contains("priority"));
    }
  }

  static final class Sample {

    @OnShutdown(priority = 7)
    void stop() {
    }
  }
}
