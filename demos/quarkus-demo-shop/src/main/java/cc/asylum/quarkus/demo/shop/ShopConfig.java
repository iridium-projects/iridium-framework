package cc.asylum.quarkus.demo.shop;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "shop")
public interface ShopConfig {
  String name();
  String currency();
}
