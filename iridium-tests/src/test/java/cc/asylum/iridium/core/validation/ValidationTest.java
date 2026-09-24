package cc.asylum.iridium.core.validation;

import cc.asylum.iridium.core.result.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationTest {

  @Test
  void rejectsNullAndPassesUnknownTypes() {
    final Result<String, List<ConstraintViolation>> missing = Validation.validate(null);
    assertTrue(missing.isErr());
    assertEquals("", missing.unwrapErr().get(0).field());
    assertEquals("must not be null", missing.unwrapErr().get(0).message());
    assertNull(missing.unwrapErr().get(0).invalidValue());

    final String value = "plain";
    assertSame(value, Validation.validate(value).unwrap());
  }

  @Test
  void escapeReturnsNullAndUnregisteredValues() {
    assertNull(Validation.escape(null));
    final String value = "plain";
    assertSame(value, Validation.escape(value));
  }

  @Test
  void registryReturnsTheRegisteredValidator() {
    final ValidatorRegistry registry = new ValidatorRegistry();
    final Validator<String> validator = value -> Result.ok(value);
    assertNull(registry.get(String.class));
    registry.register(String.class, validator);
    assertSame(validator, registry.get(String.class));
    registry.register(String.class, validator);
    assertSame(validator, registry.get(String.class));
  }

  @Test
  void resetDropsRegisteredValidators() {
    Validation.initialize();
    Validation.reset();
    assertSame("plain", Validation.validate("plain").unwrap());
  }
}
