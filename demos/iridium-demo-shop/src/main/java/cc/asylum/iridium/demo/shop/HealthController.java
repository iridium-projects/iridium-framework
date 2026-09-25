package cc.asylum.iridium.demo.shop;

import cc.asylum.iridium.demo.shop.svc.HealthService;
import cc.asylum.iridium.web.controller.RestController;
import cc.asylum.iridium.web.controller.mapping.GET;
import cc.asylum.iridium.web.response.Response;

@RestController("/api")
public final class HealthController {

  private final HealthService health;
  private final ShopConfig config;

  public HealthController(final HealthService health, final ShopConfig config) {
    this.health = health;
    this.config = config;
  }

  @GET("/health")
  public Response<String> health() {
    return Response.ok(config.name() + ":" + health.label());
  }
}
