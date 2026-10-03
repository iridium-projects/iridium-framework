package cc.asylum.iridium.core.result;

import cc.asylum.iridium.core.validation.annotation.NotNull;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public record Ok<T, E>(@NotNull T value) implements Result<T, E> {

  public Ok {
    if (value == null) {
      throw new IllegalArgumentException("Ok value must not be null");
    }
  }

  @Override
  public boolean isOk() {
    return true;
  }

  @Override
  public @NotNull T unwrap() {
    return value;
  }

  @Override
  public @NotNull T unwrapOr(final @NotNull T defaultValue) {
    return value;
  }

  @Override
  public @NotNull T unwrapOrElse(final @NotNull Function<E, T> fn) {
    return value;
  }

  @Override
  public @NotNull E unwrapErr() {
    throw new NoSuchElementException("Called unwrapErr() on Ok: " + value);
  }

  @Override
  public <U> @NotNull Result<U, E> map(final @NotNull Function<T, U> fn) {
    return Result.ok(fn.apply(value));
  }

  @Override
  public <F> @NotNull Result<T, F> mapErr(final @NotNull Function<E, F> fn) {
    return Result.ok(value);
  }

  @Override
  public <U> @NotNull Result<U, E> flatMap(final @NotNull Function<T, Result<U, E>> fn) {
    return fn.apply(value);
  }

  @Override
  public @NotNull Result<T, E> ifOk(final @NotNull Consumer<T> action) {
    action.accept(value);
    return this;
  }

  @Override
  public @NotNull Result<T, E> ifErr(final @NotNull Consumer<E> action) {
    return this;
  }

  @Override
  public @NotNull Optional<T> ok() {
    return Optional.of(value);
  }

  @Override
  public @NotNull Optional<E> err() {
    return Optional.empty();
  }
}
