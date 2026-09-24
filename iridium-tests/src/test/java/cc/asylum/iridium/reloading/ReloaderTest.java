package cc.asylum.iridium.reloading;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloaderTest {

  @Test
  void detectsGeneratedRegistrarsAndValidators() {
    assertTrue(Reloader.generated("test.gen.WebRegistrarGenerated"));
    assertTrue(Reloader.generated("test.gen.BeanRegistrarGenerated"));
    assertTrue(Reloader.generated("test.gen.ValidationRegistrarGenerated"));
    assertTrue(Reloader.generated("test.gen.HelloValidator"));
    assertFalse(Reloader.generated("test.HelloController"));
    assertFalse(Reloader.generated("ValidatorFactory"));
  }
}
