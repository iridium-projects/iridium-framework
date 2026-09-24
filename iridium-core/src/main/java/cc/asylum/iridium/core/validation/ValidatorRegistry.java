package cc.asylum.iridium.core.validation;

import cc.asylum.iridium.core.annotation.Internal;

import java.util.HashMap;
import java.util.Map;

@Internal
public final class ValidatorRegistry {

  private final Map<Class<?>, Validator<?>> validators = new HashMap<>();

  public <T> void register(final Class<T> type, final Validator<T> validator) {
    validators.put(type, validator);
  }

  public void clear() {
    validators.clear();
  }

  @SuppressWarnings("unchecked")
  public <T> Validator<T> get(final Class<T> type) {
    return (Validator<T>) validators.get(type);
  }
}
