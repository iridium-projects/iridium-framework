package cc.asylum.iridium.core.bean;

import cc.asylum.iridium.core.result.Result;
import cc.asylum.iridium.core.result.Unit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
final class BeanPoolTest {

  @AfterEach
  void reset() {
    BeanPool.instance().clear();
  }

  @Test
  void putGetAndClearByName() {
    final BeanPool pool = BeanPool.instance();
    final String bean = "alpha";
    pool.put("alpha", bean);
    assertSame(bean, pool.get("alpha"));
    assertNull(pool.get("missing"));
    pool.clear();
    assertNull(pool.get("alpha"));
  }

  @Test
  void getByTypeCachesAndRejectsMissing() {
    final BeanPool pool = BeanPool.instance();
    final String bean = "only";
    pool.put("only", bean);
    assertSame(bean, pool.get(String.class));
    assertSame(bean, pool.get(String.class));
    assertSame(bean, pool.get(CharSequence.class));
    pool.put("other", 1);
    assertEquals(1, pool.get(Integer.class));
    final IllegalStateException missing = assertThrows(IllegalStateException.class, () -> pool.get(List.class));
    assertTrue(missing.getMessage().contains(List.class.getName()));
  }

  @Test
  void getByTypeRejectsAmbiguous() {
    final BeanPool pool = BeanPool.instance();
    pool.put("a", "one");
    pool.put("b", "two");
    final IllegalStateException failure = assertThrows(IllegalStateException.class, () -> pool.get(String.class));
    assertTrue(failure.getMessage().contains("2"));
    assertThrows(IllegalStateException.class, () -> pool.get(String.class));
  }

  @Test
  void allReturnsMatchingBeans() {
    final BeanPool pool = BeanPool.instance();
    pool.put("a", "one");
    pool.put("b", 2);
    pool.put("c", "three");
    assertEquals(List.of("one", "three"), pool.all(String.class));
    assertEquals(List.of(), pool.all(List.class));
  }

  @Test
  void initializeLoadsRegistrars() {
    final Result<Unit, Exception> result = BeanPool.initialize();
    assertTrue(result.isOk());
    assertEquals(Unit.INSTANCE, result.unwrap());
    assertEquals("from-service", BeanPool.instance().get("sample"));
  }

  @Test
  void registrarWritesIntoThePool() {
    final BeanPool pool = BeanPool.instance();
    new SampleBeanRegistrar().register(pool);
    assertEquals("from-service", pool.get("sample"));
  }
}
