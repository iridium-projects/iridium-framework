package cc.asylum.iridium.web.webserver;

import cc.asylum.iridium.web.undertow.UndertowWebServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class WebServerTest {

  @Test
  void loadsUndertowFromTheServiceLoader() {
    assertInstanceOf(UndertowWebServer.class, WebServer.load());
    WebServer.load().stop();
  }
}
