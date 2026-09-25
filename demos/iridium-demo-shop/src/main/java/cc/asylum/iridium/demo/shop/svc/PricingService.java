package cc.asylum.iridium.demo.shop.svc;

import cc.asylum.iridium.config.Value;
import cc.asylum.iridium.core.component.Component;
@Component

public final class PricingService {

  private final String label;

  public PricingService(final @Value("${shop.pricing:on}") String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public int weight() {
    return 2;
  }
}
