package cc.asylum.spring.demo.shop;

import cc.asylum.spring.demo.shop.svc.CheckoutService;
import cc.asylum.spring.demo.shop.svc.TaxService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class OrderController {

  private final CheckoutService checkout;
  private final TaxService tax;

  public OrderController(final CheckoutService checkout, final TaxService tax) {
    this.checkout = checkout;
    this.tax = tax;
  }

  @PostMapping("/api/orders")
  public OrderView create(final @RequestBody @Valid OrderRequest request) {
    final long total = request.qty() * 1999L + tax.weight() + checkout.weight();
    return new OrderView("ord-1", request.sku(), request.qty(), total, request.email(), "placed");
  }
}
