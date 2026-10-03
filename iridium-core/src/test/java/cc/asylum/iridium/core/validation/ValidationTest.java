package cc.asylum.iridium.core.validation;

import cc.asylum.iridium.core.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
final class ValidationTest {

  @AfterEach
  void reset() {
    Validation.reset();
  }

  @Test
  void validateNullAndUnknownType() {
    final Result<String, List<ConstraintViolation>> missing = Validation.validate(null);
    assertTrue(missing.isErr());
    assertEquals("", missing.unwrapErr().getFirst().field());
    assertEquals("must not be null", missing.unwrapErr().getFirst().message());
    assertNull(missing.unwrapErr().getFirst().invalidValue());
    assertEquals("plain", Validation.validate("plain").unwrap());
    Validation.initialize();
    assertEquals("again", Validation.validate("again").unwrap());
  }

  @Test
  void validateUsesRegisteredValidator() {
    Validation.initialize();
    assertEquals("kept", Validation.validate("kept").unwrap());
    final Result<Marked, List<ConstraintViolation>> failed = Validation.validate(new Marked("bad"));
    assertTrue(failed.isErr());
    assertEquals("name", failed.unwrapErr().getFirst().field());
    assertEquals("ok", Validation.validate(new Marked("ok")).unwrap().value());
  }

  @Test
  void escapeUsesEscaperOrReturnsValue() {
    assertNull(Validation.escape(null));
    assertEquals("plain", Validation.escape("plain"));
    assertEquals("escaped", Validation.escape(new Marked("raw")).value());
    Validation.initialize();
    assertEquals("escaped", Validation.escape(new Marked("raw")).value());
  }

  @Test
  void initializeIsIdempotentUnderContention() throws Exception {
    final CountDownLatch ready = new CountDownLatch(2);
    final CountDownLatch start = new CountDownLatch(1);
    final AtomicInteger failures = new AtomicInteger();
    final Runnable task = () -> {
      ready.countDown();
      try {
        start.await();
        Validation.initialize();
        Validation.validate("x");
      } catch (final Exception failure) {
        failures.incrementAndGet();
      }
    };
    final Thread first = new Thread(task);
    final Thread second = new Thread(task);
    first.start();
    second.start();
    ready.await();
    start.countDown();
    first.join();
    second.join();
    assertEquals(0, failures.get());
    assertEquals("escaped", Validation.escape(new Marked("raw")).value());
  }

  @Test
  void resetReloadsRegistrars() {
    Validation.initialize();
    Validation.reset();
    assertEquals("escaped", Validation.escape(new Marked("raw")).value());
  }

  @Test
  void registrarRegistersTheValidator() {
    final ValidatorRegistry registry = new ValidatorRegistry();
    new SampleValidationRegistrar().register(registry);
    assertTrue(registry.get(Marked.class) instanceof Escaper<?>);
  }
}
