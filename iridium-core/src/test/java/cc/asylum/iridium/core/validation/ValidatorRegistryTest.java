package cc.asylum.iridium.core.validation;

import cc.asylum.iridium.core.result.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
final class ValidatorRegistryTest {

  @Mock
  private Validator<String> validator;

  @Test
  void registerGetAndClear() {
    final ValidatorRegistry registry = new ValidatorRegistry();
    when(validator.validate("ok")).thenReturn(Result.ok("ok"));
    registry.register(String.class, validator);
    assertSame(validator, registry.get(String.class));
    assertNull(registry.get(Integer.class));
    assertTrue(registry.get(String.class).validate("ok").isOk());
    verify(validator).validate("ok");
    registry.clear();
    assertNull(registry.get(String.class));
  }
}
