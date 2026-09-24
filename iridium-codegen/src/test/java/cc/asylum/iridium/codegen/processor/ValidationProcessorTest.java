package cc.asylum.iridium.codegen.processor;

import cc.asylum.iridium.codegen.ProcessorHarness;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationProcessorTest {

  @Test
  void generatesValidatorAndRegistrar() throws Exception {
    final var result = ProcessorHarness.compile(Map.of(
        "test.ValidationFixture", """
            package test;
            import cc.asylum.iridium.core.validation.annotation.NotBlank;
            import cc.asylum.iridium.core.validation.annotation.NotNull;
            public final class ValidationFixture {
              @NotNull
              @NotBlank
              private String name;
              public String getName() {
                return name;
              }
            }
            """), new ValidationProcessor());

    assertTrue(result.success(), () -> String.join("\n", result.errors()));

    final String validator =
        ProcessorHarness.generatedSource(result, "test", "gen", "ValidationFixtureValidator.java");
    assertTrue(validator.contains("must not be null"), validator);
    assertTrue(validator.contains("must not be blank"), validator);
    assertTrue(validator.contains("value.getName()"), validator);

    final String registrar =
        ProcessorHarness.generatedSource(result, "test", "gen", "ValidationRegistrarGenerated.java");
    assertTrue(registrar.contains("ValidationFixtureValidator"), registrar);
  }

  @Test
  void skipsTypesWithoutConstraints() throws Exception {
    final var result = ProcessorHarness.compile(Map.of(
        "test.Plain", """
            package test;
            public final class Plain {
              private String name;
            }
            """), new ValidationProcessor());

    assertTrue(result.success(), () -> String.join("\n", result.errors()));
  }
}
