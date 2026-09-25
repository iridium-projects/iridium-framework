package cc.asylum.iridium.demo.shop;

import cc.asylum.iridium.web.Iridium;
import cc.asylum.iridium.web.WebApplication;

@WebApplication
public final class Application {

  public static void main(final String[] args) {
    Iridium.run(Application.class, args);
  }
}
