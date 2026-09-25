package cc.asylum.iridium.core.bean;

import cc.asylum.iridium.core.annotation.Internal;
import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.result.Unit;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

@Internal
public final class BeanPool {
  private static final BeanPool INSTANCE = new BeanPool();

  private final Map<String, Object> beans = new LinkedHashMap<>();
  private final Map<Class<?>, Object> byType = new IdentityHashMap<>();

  private BeanPool() {
  }

  public void put(final String name, final Object bean) {
    beans.put(name, bean);
    byType.clear();
  }

  public void clear() {
    beans.clear();
    byType.clear();
  }

  @SuppressWarnings("unchecked")
  public <T> T get(final String name) {
    return (T) beans.get(name);
  }

  public <T> T get(final Class<T> type) {
    final Object cached = byType.get(type);
    if (cached != null) {
      if (cached == AMBIGUOUS) {
        throw ambiguous(type);
      }
      return type.cast(cached);
    }

    Object match = null;
    int count = 0;
    for (final Object bean : beans.values()) {
      if (type.isInstance(bean)) {
        count++;
        match = bean;
      }
    }

    if (count == 0) {
      throw new IllegalStateException("No Bean found for type " + type.getName());
    }
    if (count > 1) {
      byType.put(type, AMBIGUOUS);
      throw ambiguous(type);
    }

    byType.put(type, match);
    return type.cast(match);
  }

  public <T> List<T> all(final Class<T> type) {
    final List<T> matches = new ArrayList<>();
    for (final Object bean : beans.values()) {
      if (type.isInstance(bean)) {
        matches.add(type.cast(bean));
      }
    }
    return List.copyOf(matches);
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

  private static IllegalStateException ambiguous(final Class<?> type) {
    int count = 0;
    for (final Object bean : INSTANCE.beans.values()) {
      if (type.isInstance(bean)) {
        count++;
      }
    }
    return new IllegalStateException("Expected a single bean of type '"
        + type.getName() + "' but found " + count + " beans");
  }

  private static final Object AMBIGUOUS = new Object();
}
