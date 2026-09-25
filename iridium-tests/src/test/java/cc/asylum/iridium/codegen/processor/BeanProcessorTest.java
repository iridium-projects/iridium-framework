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
            import cc.asylum.iridium.core.component.Component;
            @Component
            public final class TestFactory {
              @Bean
              public TestService service() {
                return new TestService();
              }
            }
            """,
        "test.TestHooks", """
            package test;
            import cc.asylum.iridium.core.component.Component;
            import cc.asylum.iridium.core.hook.OnShutdown;
            @Component
            public final class TestHooks {
              @OnShutdown
              public void stop() {
              }
            }
            """,
        "test.TestController", """
            package test;
            import cc.asylum.iridium.web.controller.RestController;
            @RestController
            public final class TestController {
              public TestController(final TestService service) {
              }
            }
            """), new BeanProcessor());

    assertTrue(result.success(), () -> String.join("\n", result.errors()));

    final String registrar = ProcessorHarness.generatedSource(result, "test", "gen", "BeanRegistrarGenerated.java");
    assertTrue(registrar.contains("pool.put(\"testController\", new TestController(pool.get(TestService.class)))"), registrar);
    assertTrue(registrar.contains("pool.put(\"testService\", new TestService())"), registrar);
    assertTrue(registrar.contains("pool.put(\"service\", pool.get(TestFactory.class).service())"), registrar);
    assertTrue(registrar.contains("ShutdownHook"), registrar);
    assertTrue(registrar.contains("test.TestHooks.stop"), registrar);
  }
}
