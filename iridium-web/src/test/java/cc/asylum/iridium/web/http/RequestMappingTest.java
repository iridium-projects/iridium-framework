package cc.asylum.iridium.web.http;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class RequestMappingTest {

  @Test
  void annotationsAreClassRetention() {
    assertEquals(RetentionPolicy.CLASS, RequestMapping.class.getAnnotation(Retention.class).value());
    assertEquals(ElementType.METHOD, RequestMapping.class.getAnnotation(Target.class).value()[0]);
    assertEquals(RetentionPolicy.CLASS, HttpClient.class.getAnnotation(Retention.class).value());
    assertEquals(ElementType.TYPE, HttpClient.class.getAnnotation(Target.class).value()[0]);
    assertArrayEquals(
      new RequestMethod[] {
        RequestMethod.DELETE,
        RequestMethod.GET,
        RequestMethod.HEAD,
        RequestMethod.OPTIONS,
        RequestMethod.PATCH,
        RequestMethod.POST,
        RequestMethod.PUSH,
        RequestMethod.PUT
      },
      RequestMethod.values());
    assertEquals(RequestMethod.POST, RequestMethod.valueOf("POST"));
    assertEquals(8, Arrays.stream(RequestMethod.values()).count());
  }
}
