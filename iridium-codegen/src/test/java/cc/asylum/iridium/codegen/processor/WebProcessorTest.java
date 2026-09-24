package cc.asylum.iridium.codegen.processor;

import cc.asylum.iridium.codegen.ProcessorHarness;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WebProcessorTest {

  @Test
  void generatesRegistrarWithBindings() throws Exception {
    final var result = ProcessorHarness.compile(Map.of(
        "test.WebFixture", """
            package test;
            import cc.asylum.iridium.web.controller.RestController;
            import cc.asylum.iridium.web.controller.mapping.GET;
            import cc.asylum.iridium.web.controller.parameter.RequestParam;
            import cc.asylum.iridium.web.response.Response;
            @RestController
            public final class WebFixture {
              @GET("/hello")
              public Response<String> hello() {
                return Response.ok("hi");
              }
              @GET("/greet")
              public Response<String> greet(
                  @RequestParam(defaultValue = "world") String name,
                  @RequestParam(defaultValue = "3") int count,
                  @RequestParam java.util.Optional<String> salutation) {
                return Response.ok("hi");
              }
            }
            """), new WebProcessor());

    assertTrue(result.success(), () -> String.join("\n", result.errors()));

    final String registrar =
        ProcessorHarness.generatedSource(result, "test", "gen", "WebRegistrarGenerated.java");
    assertTrue(registrar.contains("router.register(\"GET\", \"/hello\""), registrar);
    assertTrue(registrar.contains("router.register(\"GET\", \"/greet\""), registrar);
    assertTrue(registrar.contains("Parameters.query(_request, \"name\", \"world\")"), registrar);
    assertTrue(registrar.contains("Integer.parseInt("), registrar);
    assertTrue(registrar.contains("Optional.ofNullable("), registrar);
  }
}
