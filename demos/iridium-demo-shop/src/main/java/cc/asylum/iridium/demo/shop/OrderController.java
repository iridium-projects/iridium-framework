package cc.asylum.iridium.demo.shop;

import cc.asylum.iridium.core.validation.Valid;
import cc.asylum.iridium.demo.shop.svc.CheckoutService;
import cc.asylum.iridium.demo.shop.svc.TaxService;
import cc.asylum.iridium.web.controller.RestController;
import cc.asylum.iridium.web.controller.mapping.POST;
import cc.asylum.iridium.web.controller.parameter.RequestBody;
import cc.asylum.iridium.web.response.Response;

@RestController("/api")
public final class OrderController {

  private final CheckoutService checkout;
  private final TaxService tax;

  public OrderController(final CheckoutService checkout, final TaxService tax) {
    this.checkout = checkout;
    this.tax = tax;
  }

  @POST("/orders")
  public Response<OrderView> create(final @RequestBody @Valid OrderRequest request) {
    final long total = request.qty() * 1999L + tax.weight() + checkout.weight();
    return Response.ok(new OrderView("ord-1", request.sku(), request.qty(), total, request.email(), "placed"));
  }
}
