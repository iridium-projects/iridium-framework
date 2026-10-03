package cc.asylum.iridium.core.result;

import cc.asylum.iridium.core.validation.annotation.NotNull;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public sealed interface Result<T, E> permits Ok, Err {

  static <T, E> @NotNull Result<T, E> ok(final @NotNull T value) {
    return new Ok<>(value);
  }

  static <T, E> @NotNull Result<T, E> err(final @NotNull E error) {
    return new Err<>(error);
  }

  boolean isOk();

  default boolean isErr() {
    return !isOk();
  }

  @NotNull
  T unwrap();

  @NotNull
  T unwrapOr(final @NotNull T defaultValue);

  @NotNull
  T unwrapOrElse(final @NotNull Function<E, T> fn);

  @NotNull
  E unwrapErr();

  <U> @NotNull Result<U, E> map(final @NotNull Function<T, U> fn);

  <F> @NotNull Result<T, F> mapErr(final @NotNull Function<E, F> fn);

  <U> @NotNull Result<U, E> flatMap(final @NotNull Function<T, Result<U, E>> fn);

  @NotNull
  Result<T, E> ifOk(final @NotNull Consumer<T> action);

  @NotNull
  Result<T, E> ifErr(final @NotNull Consumer<E> action);

  @NotNull
  Optional<T> ok();

  @NotNull
  Optional<E> err();

  static <T, X extends Throwable> @NotNull Result<T, X> of(
      final @NotNull ThrowingSupplier<T, X> supplier) {
    try {
      return ok(supplier.get());
    } catch (Throwable e) {
      @SuppressWarnings("unchecked")
      X error = (X) e;
      return err(error);
    }
  }
}
