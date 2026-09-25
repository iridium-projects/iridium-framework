package cc.asylum.quarkus.demo.shop;

import cc.asylum.quarkus.demo.shop.svc.CatalogService;
import cc.asylum.quarkus.demo.shop.svc.PricingService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api")
public class CatalogController {

  private final CatalogService catalog;
  private final PricingService pricing;
  private final ShopConfig config;

  public CatalogController(
      final CatalogService catalog,
      final PricingService pricing,
      final ShopConfig config) {
    this.catalog = catalog;
    this.pricing = pricing;
    this.config = config;
  }

  @GET
  @Path("/products/{sku}")
  @Produces(MediaType.APPLICATION_JSON)
  public Product product(final @PathParam("sku") String sku) {
    final int stock = 40 + catalog.weight() + pricing.weight();
    return new Product(sku, config.name() + " " + sku, 1999, stock);
  }
}
