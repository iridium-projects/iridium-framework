package cc.asylum.spring.demo.shop.svc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component

public final class TaxService {

  private final String label;

  public TaxService(final @Value("${shop.tax:on}") String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public int weight() {
    return 10;
  }
}
