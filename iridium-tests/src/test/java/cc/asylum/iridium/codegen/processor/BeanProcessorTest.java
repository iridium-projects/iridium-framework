package cc.asylum.iridium.codegen.processor;

import cc.asylum.iridium.codegen.ProcessorHarness;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanProcessorTest {

  @Test
  void generatesRegistrarForComponentsBeansAndHooks() throws Exception {
    final var result = ProcessorHarness.compile(Map.of(
        "test.TestService", """
            package test;
            import cc.asylum.iridium.core.component.Component;
            @Component
            public final class TestService {
            }
            """,
        "test.TestFactory", """
            package test;
            import cc.asylum.iridium.core.bean.Bean;
            public final class TestFactory {
              @Bean
              public TestService service() {
                return new TestService();
              }
            }
            """,
        "test.TestHooks", """
            package test;
            import cc.asylum.iridium.core.hook.OnShutdown;
            public final class TestHooks {
              @OnShutdown
              public void stop() {
              }
            }
            """), new BeanProcessor());

    assertTrue(result.success(), () -> String.join("\n", result.errors()));

    final String registrar = ProcessorHarness.generatedSource(result, "test", "gen", "BeanRegistrarGenerated.java");
    assertTrue(registrar.contains("pool.put(\"testService\", new TestService())"), registrar);
    assertTrue(registrar.contains("pool.put(\"service\", new TestFactory().service())"), registrar);
    assertTrue(registrar.contains("ShutdownHook"), registrar);
    assertTrue(registrar.contains("test.TestHooks.stop"), registrar);
  }
}
