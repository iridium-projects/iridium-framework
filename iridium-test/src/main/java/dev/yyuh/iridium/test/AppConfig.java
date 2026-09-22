package dev.yyuh.iridium.test;

import de.yyuh.iridium.core.bean.Bean;
import de.yyuh.iridium.core.component.Component;

@Component
public final class AppConfig {

    @Bean
    public Greeting greeting() {
        return new Greeting("Hello from @Bean");
    }

    public record Greeting(String text) {
    }
}
