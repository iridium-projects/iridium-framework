package cc.asylum.iridium.codegen.processor;

import cc.asylum.iridium.codegen.ProcessorHarness;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessorEdgeTest {

  @Test
  void rejectsDuplicateBeanNamesAcrossPackages() throws Exception {
    final var result = ProcessorHarness.compile(Map.of(
        "a.Item", """
            package a;
            import cc.asylum.iridium.core.component.Component;
            @Component
            public final class Item {}
            """,
        "b.Item", """
            package b;
            import cc.asylum.iridium.core.component.Component;
            @Component
            public final class Item {}
            """), new BeanProcessor());

    assertFalse(result.success());
    assertTrue(result.errors().stream().anyMatch(error -> error.contains("duplicate bean name 'item'")));
  }

  @Test
  void rejectsInvalidConfigurationDefault() throws Exception {
    final var result = ProcessorHarness.compile(Map.of(
        "test.BrokenConfig", """
            package test;
            import cc.asylum.iridium.config.ConfigurationProperties;
            import cc.asylum.iridium.config.Default;
            @ConfigurationProperties("broken")
            public record BrokenConfig(@Default("nope") int port) {}
            """), new BeanProcessor());

    assertFalse(result.success());
    assertTrue(result.errors().stream().anyMatch(error -> error.contains("invalid default 'nope'")));
  }

  @Test
  void skipsUnmappedMethodsAndRejectsRequestAttributes() throws Exception {
    final var skipped = ProcessorHarness.compile(Map.of(
        "test.Partial", """
            package test;
            import cc.asylum.iridium.web.controller.RestController;
            import cc.asylum.iridium.web.controller.mapping.GET;
            import cc.asylum.iridium.web.response.Response;
            @RestController
            public final class Partial {
              @GET("/ok")
              public Response<String> ok() { return Response.ok("ok"); }
              public String ignored() { return "no"; }
            }
            """), new WebProcessor());
    assertTrue(skipped.success(), () -> String.join("\n", skipped.errors()));
    final String registrar = ProcessorHarness.generatedSource(skipped, "test", "gen", "WebRegistrarGenerated.java");
    assertTrue(registrar.contains("\"/ok\""));
    assertFalse(registrar.contains("ignored"));

    final var attributes = ProcessorHarness.compile(Map.of(
        "test.Attrs", """
            package test;
            import cc.asylum.iridium.web.controller.RestController;
            import cc.asylum.iridium.web.controller.mapping.GET;
            import cc.asylum.iridium.web.controller.parameter.RequestAttribute;
            import cc.asylum.iridium.web.response.Response;
            @RestController
            public final class Attrs {
              @GET("/attr")
              public Response<String> attr(@RequestAttribute("user") String user) {
                return Response.ok(user);
              }
            }
            """), new WebProcessor());
    assertFalse(attributes.success());
    assertTrue(attributes.errors().stream().anyMatch(error -> error.contains("@RequestAttribute is not supported yet")));
  }

  @Test
  void rejectsPastOnAString() throws Exception {
    final var result = ProcessorHarness.compile(Map.of(
        "test.When", """
            package test;
            import cc.asylum.iridium.core.validation.annotation.Past;
            public final class When {
              @Past
              private int born;
              public int getBorn() { return born; }
            }
            """), new ValidationProcessor());
    assertFalse(result.success(), () -> String.join("\n", result.errors()));
    assertTrue(result.errors().stream().anyMatch(error -> error.contains("@Past/@Future requires")),
        () -> String.join("\n", result.errors()));
  }
}
