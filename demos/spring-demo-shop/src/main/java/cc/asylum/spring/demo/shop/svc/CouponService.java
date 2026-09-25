package cc.asylum.spring.demo.shop.svc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component

public final class CouponService {

  private final String label;

  public CouponService(final @Value("${shop.coupon:on}") String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public int weight() {
    return 12;
  }
}
