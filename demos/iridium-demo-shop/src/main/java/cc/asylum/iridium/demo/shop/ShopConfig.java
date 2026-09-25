package cc.asylum.iridium.demo.shop;

import cc.asylum.iridium.config.ConfigurationProperties;

@ConfigurationProperties("shop")
public record ShopConfig(String name, String currency) {
}
