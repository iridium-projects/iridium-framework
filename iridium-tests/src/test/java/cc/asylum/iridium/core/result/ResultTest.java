package cc.asylum.iridium.core.result;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

  @Test
  void rejectsNullOkAndErr() {
    assertThrows(IllegalArgumentException.class, () -> Result.ok(null));
    assertThrows(IllegalArgumentException.class, () -> Result.err(null));
  }

  @Test
  void unwrapsOkAndErr() {
    final Result<String, String> ok = Result.ok("value");
    final Result<String, String> err = Result.err("boom");

    assertTrue(ok.isOk());
    assertFalse(ok.isErr());
    assertEquals("value", ok.unwrap());
    assertEquals("value", ok.unwrapOr("fallback"));
    assertEquals("value", ok.unwrapOrElse(error -> "mapped"));
    assertEquals(Optional.of("value"), ok.ok());
    assertEquals(Optional.empty(), ok.err());
    assertThrows(NoSuchElementException.class, ok::unwrapErr);

    assertFalse(err.isOk());
    assertTrue(err.isErr());
    assertEquals("boom", err.unwrapErr());
    assertEquals("fallback", err.unwrapOr("fallback"));
    assertEquals("mapped", err.unwrapOrElse(error -> "mapped"));
    assertEquals(Optional.empty(), err.ok());
    assertEquals(Optional.of("boom"), err.err());
    final NoSuchElementException thrown = assertThrows(NoSuchElementException.class, err::unwrap);
    assertTrue(thrown.getMessage().contains("boom"));
  }

  @Test
  void mapsOnlyTheActiveSide() {
    final Result<String, String> ok = Result.ok("value");
    final Result<String, String> err = Result.err("boom");

    assertEquals("VALUE", ok.map(String::toUpperCase).unwrap());
    assertEquals("boom", err.map(String::toUpperCase).unwrapErr());
    assertEquals("value", ok.mapErr(String::toUpperCase).unwrap());
    assertEquals("BOOM", err.mapErr(String::toUpperCase).unwrapErr());
    assertEquals("flat", ok.flatMap(value -> Result.ok("flat")).unwrap());
    assertEquals("nested", ok.flatMap(value -> Result.<String, String>err("nested")).unwrapErr());
    assertEquals("boom", err.flatMap(value -> Result.ok("flat")).unwrapErr());
    assertThrows(IllegalArgumentException.class, () -> ok.map(value -> null));
    assertThrows(IllegalArgumentException.class, () -> err.mapErr(error -> null));
  }

  @Test
  void runsOnlyTheMatchingSideEffect() {
    final AtomicBoolean okSeen = new AtomicBoolean();
    final AtomicBoolean errSeen = new AtomicBoolean();
    final Result<String, String> ok = Result.ok("value");
    final Result<String, String> err = Result.err("boom");

    assertSame(ok, ok.ifOk(value -> okSeen.set(true)).ifErr(error -> errSeen.set(true)));
    assertTrue(okSeen.get());
    assertFalse(errSeen.get());

    okSeen.set(false);
    assertSame(err, err.ifOk(value -> okSeen.set(true)).ifErr(error -> errSeen.set(true)));
    assertFalse(okSeen.get());
    assertTrue(errSeen.get());
  }

  @Test
  void capturesThrownExceptions() {
    final Result<String, IOException> checked = Result.of(() -> {
      throw new IOException("disk");
    });
    assertTrue(checked.isErr());
    assertEquals("disk", checked.unwrapErr().getMessage());

    final Result<String, IllegalStateException> unchecked = Result.of(() -> {
      throw new IllegalStateException("state");
    });
    assertEquals("state", unchecked.unwrapErr().getMessage());

    assertEquals("ok", Result.of(() -> "ok").unwrap());
    final Result<String, Throwable> nulled = Result.of(() -> null);
    assertTrue(nulled.isErr());
    assertInstanceOf(IllegalArgumentException.class, nulled.unwrapErr());
  }

  @Test
  void unitIsASingletonRecord() {
    assertEquals(Unit.INSTANCE, new Unit());
  }
}
