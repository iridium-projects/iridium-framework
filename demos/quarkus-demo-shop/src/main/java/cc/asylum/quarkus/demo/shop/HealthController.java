package cc.asylum.quarkus.demo.shop;

import cc.asylum.quarkus.demo.shop.svc.HealthService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api")
public class HealthController {

  private final HealthService health;
  private final ShopConfig config;

  public HealthController(final HealthService health, final ShopConfig config) {
    this.health = health;
    this.config = config;
  }

  @GET
  @Path("/health")
  @Produces(MediaType.TEXT_PLAIN)
  public String health() {
    return config.name() + ":" + health.label();
  }
}
