package cc.asylum.iridium.core.validation;

import cc.asylum.iridium.core.result.Result;

import java.util.List;
import java.util.ServiceLoader;

public final class Validation {

  private static final ValidatorRegistry REGISTRY = new ValidatorRegistry();
  private static volatile boolean initialized = false;

  private Validation() {
  }

  public static void reset() {
    synchronized (Validation.class) {
      REGISTRY.clear();
      initialized = false;
    }
  }

  public static void initialize() {
    if (initialized) {
      return;
    }
    synchronized (Validation.class) {
      if (initialized) {
        return;
      }
      REGISTRY.clear();
      for (final ValidationRegistrar registrar : ServiceLoader.load(ValidationRegistrar.class)) {
        registrar.register(REGISTRY);
      }
      initialized = true;
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> Result<T, List<ConstraintViolation>> validate(final T value) {
    initialize();

    if (value == null) {
      return Result.err(List.of(new ConstraintViolation("", "must not be null", null)));
    }

    final Validator<T> validator = REGISTRY.get((Class<T>) value.getClass());
    if (validator == null) {
      return Result.ok(value);
    }

    return validator.validate(value);
  }

  @SuppressWarnings("unchecked")
  public static <T> T escape(final T value) {
    initialize();

    if (value == null) {
      return null;
    }

    final Validator<T> validator = REGISTRY.get((Class<T>) value.getClass());

    if (validator instanceof final Escaper<?> escaper) {
      final Escaper<T> typed = (Escaper<T>) escaper;
      return typed.escape(value);
    }

    return value;
  }
}
