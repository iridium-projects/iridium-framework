package cc.asylum.spring.demo.hello;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
}
