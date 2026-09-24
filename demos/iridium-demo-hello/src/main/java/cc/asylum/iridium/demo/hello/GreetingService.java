package cc.asylum.iridium.demo.hello;

import cc.asylum.iridium.config.Value;
import cc.asylum.iridium.core.component.Component;
import cc.asylum.iridium.core.hook.OnShutdown;

@Component
public final class GreetingService {

  private final GreetingConfig config;
  private final String suffix;

  public GreetingService(
      final GreetingConfig config,
      final @Value("${greeting.suffix:!}") String suffix) {
    this.config = config;
    this.suffix = suffix;
  }

  public String greet(final String name) {
    return config.prefix() + ", " + name + suffix;
  }

  @OnShutdown
  public void stop() {
    System.out.println("Goodbye!");
  }
}
