package cc.asylum.iridium.core.result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
final class ResultTest {

  @Mock
  private Function<String, Integer> mapper;

  @Mock
  private Function<String, String> errorMapper;

  @Mock
  private Function<String, Result<Integer, String>> flatMapper;

  @Mock
  private Consumer<String> action;

  @Test
  void okCarriesTheValue() {
    final Result<String, String> result = Result.ok("value");
    assertInstanceOf(Ok.class, result);
    assertTrue(result.isOk());
    assertFalse(result.isErr());
    assertEquals("value", result.unwrap());
    assertEquals("value", result.unwrapOr("fallback"));
    assertEquals("value", result.unwrapOrElse(ignored -> "fallback"));
    assertEquals(Optional.of("value"), result.ok());
    assertEquals(Optional.empty(), result.err());
    assertSame(result, result.ifOk(action));
    verify(action).accept("value");
    assertSame(result, result.ifErr(ignored -> {
      throw new AssertionError("err action");
    }));
  }

  @Test
  void okRejectsNullAndUnwrapErr() {
    assertThrows(IllegalArgumentException.class, () -> new Ok<String, String>(null));
    final Result<String, String> result = Result.ok("value");
    final NoSuchElementException failure = assertThrows(NoSuchElementException.class, result::unwrapErr);
    assertTrue(failure.getMessage().contains("value"));
  }

  @Test
  void okMapsAndFlattens(@Mock final Function<String, Integer> unused) {
    when(mapper.apply("value")).thenReturn(7);
    when(flatMapper.apply("value")).thenReturn(Result.ok(9));
    final Result<String, String> result = Result.ok("value");
    assertEquals(7, result.map(mapper).unwrap());
    assertEquals("value", result.mapErr(errorMapper).unwrap());
    verifyNoInteractions(errorMapper);
    assertEquals(9, result.flatMap(flatMapper).unwrap());
    verify(mapper).apply("value");
    verifyNoInteractions(unused);
  }

  @Test
  void errCarriesTheError() {
    final Result<String, String> result = Result.err("boom");
    assertInstanceOf(Err.class, result);
    assertFalse(result.isOk());
    assertTrue(result.isErr());
    assertEquals("fallback", result.unwrapOr("fallback"));
    assertEquals("mapped", result.unwrapOrElse(error -> "mapped"));
    assertEquals("boom", result.unwrapErr());
    assertEquals(Optional.empty(), result.ok());
    assertEquals(Optional.of("boom"), result.err());
    assertSame(result, result.ifErr(action));
    verify(action).accept("boom");
    assertSame(result, result.ifOk(ignored -> {
      throw new AssertionError("ok action");
    }));
  }

  @Test
  void errRejectsNullAndUnwrap() {
    assertThrows(IllegalArgumentException.class, () -> new Err<String, String>(null));
    final Result<String, String> result = Result.err("boom");
    final NoSuchElementException failure = assertThrows(NoSuchElementException.class, result::unwrap);
    assertTrue(failure.getMessage().contains("boom"));
  }

  @Test
  void errMapsErrorAndSkipsValue() {
    when(errorMapper.apply("boom")).thenReturn("mapped");
    final Result<String, String> result = Result.err("boom");
    assertTrue(result.map(mapper).isErr());
    assertEquals("mapped", result.mapErr(errorMapper).unwrapErr());
    assertEquals("boom", result.flatMap(flatMapper).unwrapErr());
    verifyNoInteractions(mapper, flatMapper);
  }

  @Test
  void ofCapturesSuccessAndFailure() {
    final Result<String, Throwable> success = Result.of(() -> "ok");
    assertEquals("ok", success.unwrap());
    final IllegalStateException cause = new IllegalStateException("nope");
    final Result<String, Throwable> failure = Result.of(() -> {
      throw cause;
    });
    assertSame(cause, failure.unwrapErr());
    final AtomicReference<String> seen = new AtomicReference<>();
    Result.ok("kept").ifOk(seen::set).ifErr(ignored -> seen.set("err"));
    assertEquals("kept", seen.get());
  }

  @Test
  void unitIsASingleton() {
    assertEquals(Unit.INSTANCE, new Unit());
    assertEquals(Unit.INSTANCE, Result.ok(Unit.INSTANCE).unwrap());
  }
}
