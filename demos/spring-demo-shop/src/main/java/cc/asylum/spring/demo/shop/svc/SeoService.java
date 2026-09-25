package cc.asylum.spring.demo.shop.svc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component

public final class SeoService {

  private final String label;

  public SeoService(final @Value("${shop.seo:on}") String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public int weight() {
    return 35;
  }
}
