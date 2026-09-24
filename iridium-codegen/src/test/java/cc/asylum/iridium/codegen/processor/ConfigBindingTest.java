package cc.asylum.iridium.codegen.processor;

import cc.asylum.iridium.codegen.ProcessorHarness;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigBindingTest {

  @Test
  void registersConfigurationPropertiesAndValueInjection() throws Exception {
    final var result = ProcessorHarness.compile(Map.of(
        "test.ServerConfig", """
            package test;
            import cc.asylum.iridium.config.ConfigurationProperties;
            import cc.asylum.iridium.config.Default;
            import cc.asylum.iridium.core.validation.annotation.Nullable;
            @ConfigurationProperties("server")
            public record ServerConfig(String host, @Default("8080") int port, @Nullable Ssl ssl, java.util.List<Peer> peers) {
              public record Ssl(boolean enabled) {}
              public record Peer(String host) {}
            }
            """,
        "test.Level", """
            package test;
            public enum Level { INFO, DEBUG }
            """,
        "test.AppService", """
            package test;
            import cc.asylum.iridium.config.Value;
            import cc.asylum.iridium.core.component.Component;
            @Component
            public final class AppService {
              public AppService(ServerConfig server, @Value("${app.name:iridium}") String name,
                  @Value("${app.level:INFO}") Level level) {
              }
            }
            """), new BeanProcessor());

    assertTrue(result.success(), () -> String.join("\n", result.errors()));
    final String registrar = ProcessorHarness.generatedSource(result, "test", "gen", "BeanRegistrarGenerated.java");
    assertTrue(registrar.contains("new ServerConfig("), registrar);
    assertTrue(registrar.contains(".unwrap()"), registrar);
    assertTrue(registrar.contains("Config.string(\"server.host\")"), registrar);
    assertTrue(registrar.contains("Config.integer(\"server.port\", 8080)"), registrar);
    assertTrue(registrar.contains("Config.bool(\"server.ssl.enabled\")"), registrar);
    assertTrue(registrar.contains("Config.indexed(\"server.peers\""), registrar);
    assertTrue(registrar.contains("Config.string(\"app.name\", \"iridium\")"), registrar);
    assertTrue(registrar.contains("Config.enumeration(\"app.level\", Level.class, Level.INFO)"), registrar);
    assertTrue(registrar.indexOf("serverConfig") < registrar.indexOf("appService"), registrar);
  }

  @Test
  void rejectsComponentAndConfigurationProperties() throws Exception {
    final var result = ProcessorHarness.compile(Map.of(
        "test.Broken", """
            package test;
            import cc.asylum.iridium.config.ConfigurationProperties;
            import cc.asylum.iridium.core.component.Component;
            @Component
            @ConfigurationProperties("broken")
            public record Broken(String name) {}
            """), new BeanProcessor());
    assertFalse(result.success());
    assertTrue(result.errors().stream().anyMatch(error -> error.contains("@ConfigurationProperties cannot be combined")));
  }
}
