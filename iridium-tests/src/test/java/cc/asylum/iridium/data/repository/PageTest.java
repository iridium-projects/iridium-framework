package cc.asylum.iridium.data.repository;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageTest {

  @Test
  void copiesContentAndComputesPageCount() {
    final List<String> source = new ArrayList<>(List.of("a"));
    final Page<String> page = new Page<>(source, 0, 2, 5);
    source.add("b");

    assertEquals(List.of("a"), page.content());
    assertEquals(3, page.pages());
    assertEquals(0, new Page<>(List.of(), 0, 1, 0).pages());
    assertEquals(1, new Page<>(List.of("a"), 2, 10, 1).pages());
    assertThrows(UnsupportedOperationException.class, () -> page.content().add("c"));
  }

  @Test
  void rejectsInvalidPageAndSize() {
    assertThrows(IllegalArgumentException.class, () -> new Page<>(List.of(), -1, 1, 0));
    assertThrows(IllegalArgumentException.class, () -> new Page<>(List.of(), 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new Page<>(List.of(), 0, -1, 0));
    assertThrows(NullPointerException.class, () -> new Page<>(null, 0, 1, 0));
  }
}
