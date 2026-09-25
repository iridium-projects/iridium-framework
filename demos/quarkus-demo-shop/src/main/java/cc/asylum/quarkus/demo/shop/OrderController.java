package cc.asylum.quarkus.demo.shop;

import cc.asylum.quarkus.demo.shop.svc.CheckoutService;
import cc.asylum.quarkus.demo.shop.svc.TaxService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api")
public class OrderController {

  private final CheckoutService checkout;
  private final TaxService tax;

  public OrderController(final CheckoutService checkout, final TaxService tax) {
    this.checkout = checkout;
    this.tax = tax;
  }

  @POST
  @Path("/orders")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public OrderView create(final @Valid OrderRequest request) {
    final long total = request.qty() * 1999L + tax.weight() + checkout.weight();
    return new OrderView("ord-1", request.sku(), request.qty(), total, request.email(), "placed");
  }
}
