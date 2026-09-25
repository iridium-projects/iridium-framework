package cc.asylum.iridium.demo.hello;

import cc.asylum.iridium.core.validation.Valid;
import cc.asylum.iridium.web.controller.RestController;
import cc.asylum.iridium.web.controller.mapping.GET;
import cc.asylum.iridium.web.controller.mapping.POST;
import cc.asylum.iridium.web.controller.parameter.PathVariable;
import cc.asylum.iridium.web.controller.parameter.RequestBody;
import cc.asylum.iridium.web.controller.parameter.RequestParam;
import cc.asylum.iridium.web.response.Response;

@RestController("/api")
public final class HelloController {

  private final GreetingService service;

  public HelloController(final GreetingService service) {
    this.service = service;
  }

  @GET("/hello")
  public Response<String> hello(final @RequestParam(defaultValue = "world") String name) {
    return Response.ok(service.greet(name));
  }

  @GET("/hello/{name}")
  public Response<String> helloPath(final @PathVariable("name") String name) {
    return Response.ok(service.greet(name));
  }

  @POST("/echo")
  public Response<HelloDto> echo(final @RequestBody @Valid HelloDto helloDto) {
    return Response.ok(helloDto);
  }
}
