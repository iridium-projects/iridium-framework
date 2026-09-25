package cc.asylum.spring.demo.shop.svc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component

public final class ReviewService {

  private final String label;

  public ReviewService(final @Value("${shop.review:on}") String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public int weight() {
    return 18;
  }
}
