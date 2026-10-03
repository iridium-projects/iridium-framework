package cc.asylum.iridium.config;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class ConfigurationPropertiesTest {

  @Test
  void retainedOnTypesWithEmptyDefault() throws Exception {
    final Retention retention = ConfigurationProperties.class.getAnnotation(Retention.class);
    final Target target = ConfigurationProperties.class.getAnnotation(Target.class);
    assertEquals(RetentionPolicy.RUNTIME, retention.value());
    assertArrayEquals(new ElementType[] { ElementType.TYPE }, target.value());

    final ConfigurationProperties empty = Bare.class.getAnnotation(ConfigurationProperties.class);
    final ConfigurationProperties prefixed = Prefixed.class.getAnnotation(ConfigurationProperties.class);
    assertEquals("", empty.value());
    assertEquals("server", prefixed.value());
  }

  @ConfigurationProperties
  private static final class Bare {
  }

  @ConfigurationProperties("server")
  private static final class Prefixed {
  }
}
