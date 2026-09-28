package cc.asylum.iridium.demo.hello;

import cc.asylum.iridium.web.Iridium;
import cc.asylum.iridium.web.WebApplication;

@WebApplication
public final class Application {

  static void main(final String[] args) {
    Iridium.run(Application.class, args);
  }
}
