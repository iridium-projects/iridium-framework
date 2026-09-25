package cc.asylum.spring.demo.shop;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("shop")
public record ShopConfig(String name, String currency) {
}
