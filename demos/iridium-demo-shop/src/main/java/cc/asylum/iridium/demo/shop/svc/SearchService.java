package cc.asylum.iridium.demo.shop.svc;

import cc.asylum.iridium.config.Value;
import cc.asylum.iridium.core.component.Component;
@Component

public final class SearchService {

  private final String label;

  public SearchService(final @Value("${shop.search:on}") String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public int weight() {
    return 16;
  }
}
