package cc.asylum.iridium.core.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class ListsTest {

  @Test
  void firstOfList() {
    assertNull(Lists.first((List<String>) null));
    assertNull(Lists.first(List.of()));
    assertEquals("a", Lists.first(List.of("a", "b")));
  }

  @Test
  void firstOfCollection() {
    assertNull(Lists.first((Set<String>) null));
    assertNull(Lists.first(Set.of()));
    final Set<String> values = new LinkedHashSet<>();
    values.add("a");
    values.add("b");
    assertEquals("a", Lists.first(values));
  }
}
