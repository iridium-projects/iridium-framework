package cc.asylum.spring.demo.shop;

import cc.asylum.spring.demo.shop.svc.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class HealthController {

  private final HealthService health;
  private final ShopConfig config;

  public HealthController(final HealthService health, final ShopConfig config) {
    this.health = health;
    this.config = config;
  }

  @GetMapping("/api/health")
  public String health() {
    return config.name() + ":" + health.label();
  }
}
