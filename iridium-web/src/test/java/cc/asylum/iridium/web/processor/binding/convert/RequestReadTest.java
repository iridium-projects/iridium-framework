package cc.asylum.iridium.web.processor.binding.convert;

import cc.asylum.iridium.web.controller.parameter.BindingSource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class RequestReadTest {

  @Test
  void labelsAndConstructor() throws Exception {
    final Constructor<RequestRead> constructor = RequestRead.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    try {
      constructor.newInstance();
    } catch (final InvocationTargetException ex) {
      assertNotNull(ex.getCause());
    }
    assertEquals("path variable", RequestRead.label(BindingSource.PATH));
    assertEquals("header", RequestRead.label(BindingSource.HEADER));
    assertEquals("cookie", RequestRead.label(BindingSource.COOKIE));
    assertEquals("request body", RequestRead.label(BindingSource.BODY));
    assertEquals("query parameter", RequestRead.label(BindingSource.QUERY));
  }
}
