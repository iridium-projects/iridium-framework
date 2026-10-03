package cc.asylum.iridium.core.result;

import cc.asylum.iridium.core.validation.annotation.NotNull;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public record Err<T, E>(@NotNull E error) implements Result<T, E> {

  public Err {
    if (error == null) {
      throw new IllegalArgumentException("Err value must not be null");
    }
  }

  @Override
  public boolean isOk() {
    return false;
  }

  @Override
  public @NotNull T unwrap() {
    throw new NoSuchElementException("Called unwrap() on Err: " + error);
  }

  @Override
  public @NotNull T unwrapOr(final @NotNull T defaultValue) {
    return defaultValue;
  }

  @Override
  public @NotNull T unwrapOrElse(final @NotNull Function<E, T> fn) {
    return fn.apply(error);
  }

  @Override
  public @NotNull E unwrapErr() {
    return error;
  }

  @Override
  public <U> @NotNull Result<U, E> map(final @NotNull Function<T, U> fn) {
    return Result.err(error);
  }

  @Override
  public <F> @NotNull Result<T, F> mapErr(final @NotNull Function<E, F> fn) {
    return Result.err(fn.apply(error));
  }

  @Override
  public <U> @NotNull Result<U, E> flatMap(final @NotNull Function<T, Result<U, E>> fn) {
    return Result.err(error);
  }

  @Override
  public @NotNull Result<T, E> ifOk(final @NotNull Consumer<T> action) {
    return this;
  }

  @Override
  public @NotNull Result<T, E> ifErr(final @NotNull Consumer<E> action) {
    action.accept(error);
    return this;
  }

  @Override
  public @NotNull Optional<T> ok() {
    return Optional.empty();
  }

  @Override
  public @NotNull Optional<E> err() {
    return Optional.of(error);
  }
}
