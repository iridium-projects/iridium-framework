package cc.asylum.spring.demo.shop;

import cc.asylum.spring.demo.shop.svc.CatalogService;
import cc.asylum.spring.demo.shop.svc.PricingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
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

  @GetMapping("/api/products/{sku}")
  public Product product(final @PathVariable("sku") String sku) {
    final int stock = 40 + catalog.weight() + pricing.weight();
    return new Product(sku, config.name() + " " + sku, 1999, stock);
  }
}
