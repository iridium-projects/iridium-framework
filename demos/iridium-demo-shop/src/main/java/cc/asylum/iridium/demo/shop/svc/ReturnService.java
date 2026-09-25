package cc.asylum.iridium.demo.shop.svc;

import cc.asylum.iridium.config.Value;
import cc.asylum.iridium.core.component.Component;
@Component

public final class ReturnService {

  private final String label;

  public ReturnService(final @Value("${shop.returns:on}") String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public int weight() {
    return 29;
  }
}
