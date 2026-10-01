package cc.asylum.iridium.web.router;

import cc.asylum.iridium.web.response.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class HandlerTest {

  @Test
  void defaultDoesNotReadBody() throws Exception {
    final Handler handler = request -> Response.ok(request.method());
    assertFalse(handler.readsBody());
    assertEquals("GET", handler.handle(new Request("GET", "/", Map.of(), Map.of(), Map.of(), null)).body());
  }

  @Test
  void checkedExceptionsPropagate() {
    final Handler handler = request -> {
      throw new Exception("checked");
    };
    assertThrows(Exception.class, () -> handler.handle(null));
  }
}
