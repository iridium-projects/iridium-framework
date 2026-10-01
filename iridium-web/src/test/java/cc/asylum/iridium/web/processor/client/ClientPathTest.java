package cc.asylum.iridium.web.processor.client;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ClientPathTest {

  @Test
  void privateConstructor() throws Exception {
    final Constructor<ClientPath> constructor = ClientPath.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    try {
      assertNotNull(constructor.newInstance());
    } catch (final InvocationTargetException ex) {
      assertNotNull(ex.getCause());
    }
  }
}
