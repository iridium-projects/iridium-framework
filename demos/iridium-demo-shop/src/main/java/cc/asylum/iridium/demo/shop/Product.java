package cc.asylum.iridium.demo.shop;

import io.avaje.jsonb.Json;

@Json
public record Product(String sku, String name, long cents, int stock) {
}
