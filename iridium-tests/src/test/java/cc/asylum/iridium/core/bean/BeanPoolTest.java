package cc.asylum.iridium.core.bean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanPoolTest {

  @AfterEach
  void clearPool() {
    BeanPool.instance().clear();
  }

  @Test
  void storesAndLooksUpByNameAndType() {
    final BeanPool pool = BeanPool.instance();
    final Alpha alpha = new Alpha();
    pool.put("alpha", alpha);

    assertSame(alpha, pool.get("alpha"));
    assertSame(alpha, pool.get(Alpha.class));
    assertSame(alpha, pool.get(Marker.class));
    assertNull(pool.get("missing"));
    assertThrows(IllegalStateException.class, () -> pool.get(Missing.class));
    assertEquals(List.of(alpha), pool.all(Alpha.class));
    assertTrue(pool.all(Missing.class).isEmpty());
  }

  @Test
  void overwritesByNameAndRejectsAmbiguousTypes() {
    final BeanPool pool = BeanPool.instance();
    final Beta first = new Beta();
    final Beta second = new Beta();
    pool.put("beta", first);
    pool.put("beta", second);
    pool.put("beta-other", first);

    assertSame(second, pool.get("beta"));
    assertThrows(IllegalStateException.class, () -> pool.get(Beta.class));
    assertEquals(2, pool.all(Beta.class).size());
  }

  @Test
  void clearDropsStoredBeans() {
    final BeanPool pool = BeanPool.instance();
    pool.put("alpha", new Alpha());
    pool.clear();

    assertNull(pool.get("alpha"));
    assertThrows(IllegalStateException.class, () -> pool.get(Alpha.class));
  }

  @Test
  void allowsASingleNullKey() {
    final BeanPool pool = BeanPool.instance();
    pool.put(null, "nil");
    assertEquals("nil", pool.<String>get((String) null));
  }

  private interface Marker {
  }

  private static final class Alpha implements Marker {
  }

  private static final class Beta {
  }

  private static final class Missing {
  }
}
