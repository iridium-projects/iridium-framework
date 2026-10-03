package cc.asylum.iridium.web.processor.client;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GeneratedMethodTest {

  @Test
  void holdsNameAndConfigurer() {
    final AtomicBoolean ran = new AtomicBoolean();
    final GeneratedMethod method = new GeneratedMethod("get", builder -> ran.set(true));
    assertEquals("get", method.name());
    method.configure().accept(null);
    assertTrue(ran.get());
  }
}
