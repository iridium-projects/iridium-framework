package cc.asylum.iridium.demo.shop;

import cc.asylum.iridium.demo.shop.svc.CatalogService;
import cc.asylum.iridium.demo.shop.svc.PricingService;
import cc.asylum.iridium.web.controller.RestController;
import cc.asylum.iridium.web.controller.mapping.GET;
import cc.asylum.iridium.web.controller.parameter.PathVariable;
import cc.asylum.iridium.web.response.Response;

@RestController("/api")
public final class CatalogController {

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

  @GET("/products/{sku}")
  public Response<Product> product(final @PathVariable("sku") String sku) {
    final int stock = 40 + catalog.weight() + pricing.weight();
    return Response.ok(new Product(sku, config.name() + " " + sku, 1999, stock));
  }
}
