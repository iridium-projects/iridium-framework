package cc.asylum.iridium.codegen;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AnnotationsTest {

  @Test
  void generatedDefaults() throws Exception {
    final Method author = Generated.class.getMethod("author");
    final Method date = Generated.class.getMethod("date");
    assertEquals("", author.getDefaultValue());
    assertEquals("", date.getDefaultValue());
    assertEquals(RetentionPolicy.RUNTIME, Generated.class.getAnnotation(Retention.class).value());
  }

  @Test
  void serviceDefaults() throws Exception {
    assertEquals(0, ((Class<?>[]) Service.class.getMethod("value").getDefaultValue()).length);
    assertEquals(0, Service.class.getMethod("order").getDefaultValue());
    assertEquals(false, Service.class.getMethod("isolating").getDefaultValue());
  }

  @Test
  void registerDefault() throws Exception {
    assertEquals("", Register.class.getMethod("suffix").getDefaultValue());
    assertEquals(RetentionPolicy.CLASS, Register.class.getAnnotation(Retention.class).value());
  }
}
