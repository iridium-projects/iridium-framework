package cc.asylum.iridium.config;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class DefaultTest {

  @Test
  void retainedOnParametersAndRecordComponents() throws Exception {
    final Retention retention = Default.class.getAnnotation(Retention.class);
    final Target target = Default.class.getAnnotation(Target.class);
    assertEquals(RetentionPolicy.RUNTIME, retention.value());
    assertArrayEquals(new ElementType[] { ElementType.PARAMETER, ElementType.RECORD_COMPONENT }, target.value());

    final Method method = Holder.class.getDeclaredMethod("accept", String.class);
    final Parameter parameter = method.getParameters()[0];
    assertEquals("fallback", parameter.getAnnotation(Default.class).value());

    final RecordComponent component = Sample.class.getRecordComponents()[0];
    assertEquals("8080", component.getAnnotation(Default.class).value());
  }

  private record Sample(@Default("8080") String port) {
  }

  private static final class Holder {

    @SuppressWarnings("unused")
    void accept(@Default("fallback") final String value) {
    }
  }
}
