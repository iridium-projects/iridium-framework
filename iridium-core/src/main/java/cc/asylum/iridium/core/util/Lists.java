package cc.asylum.iridium.core.util;

import cc.asylum.iridium.core.annotation.Internal;

import java.util.Collection;
import java.util.List;

@Internal
public final class Lists {

  private Lists() {
  }

  public static <T> T first(final List<T> values) {
    return values == null || values.isEmpty() ? null : values.get(0);
  }

  public static <T> T first(final Collection<T> values) {
    return values == null || values.isEmpty() ? null : values.iterator().next();
  }
}
