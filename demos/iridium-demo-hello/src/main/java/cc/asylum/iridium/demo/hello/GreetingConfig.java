package cc.asylum.iridium.demo.hello;

import cc.asylum.iridium.config.ConfigurationProperties;

@ConfigurationProperties("greeting")
public record GreetingConfig(String prefix) {
}
