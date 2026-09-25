package cc.asylum.iridium.core.bean;

import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.result.Unit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

@Internal
public final class BeanPool {
  private static final BeanPool INSTANCE = new BeanPool();

  private final Map<String, Object> beans = new LinkedHashMap<>();

  private BeanPool() {
  }

  public void put(final String name, final Object bean) {
    beans.put(name, bean);
  }

  public void clear() {
    beans.clear();
  }

  @SuppressWarnings("unchecked")
  public <T> T get(final String name) {
    return (T) beans.get(name);
  }

  public <T> T get(final Class<T> type) {
    final List<T> matches = beans.values().stream()
        .filter(type::isInstance)
        .map(type::cast)
        .toList();

    if (matches.isEmpty()) {
      throw new IllegalStateException("No Bean found for type " + type.getName());
    }

    if (matches.size() > 1) {
      throw new IllegalStateException("Expected a single bean of type '"
          + type.getName() + "' but found " + matches.size() + " beans");
    }

    return matches.get(0);
  }

  public <T> List<T> all(final Class<T> type) {
    return beans.values().stream()
        .filter(type::isInstance)
        .map(type::cast)
        .toList();
  }

  public static Result<Unit, Exception> initialize() {
    return Result.of(() -> {
      INSTANCE.clear();
      for (final BeanRegistrar registrar : ServiceLoader.load(BeanRegistrar.class)) {
        registrar.register(INSTANCE);
      }

      return Unit.INSTANCE;
    });
  }

  public static BeanPool instance() {
    return INSTANCE;
  }
}
