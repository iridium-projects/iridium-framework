package cc.asylum.spring.demo.hello;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("greeting")
public record GreetingConfig(String prefix) {
}
