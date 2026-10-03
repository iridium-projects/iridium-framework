package cc.asylum.iridium.config;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class ValueTest {

  @Test
  void retainedOnParameters() throws Exception {
    final Retention retention = Value.class.getAnnotation(Retention.class);
    final Target target = Value.class.getAnnotation(Target.class);
    assertEquals(RetentionPolicy.RUNTIME, retention.value());
    assertArrayEquals(new ElementType[] { ElementType.PARAMETER }, target.value());

    final Method method = Holder.class.getDeclaredMethod("accept", String.class);
    final Parameter parameter = method.getParameters()[0];
    assertEquals("${app.name}", parameter.getAnnotation(Value.class).value());
  }

  private static final class Holder {

    @SuppressWarnings("unused")
    void accept(@Value("${app.name}") final String name) {
    }
  }
}
