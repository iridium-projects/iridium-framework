package cc.asylum.iridium.demo.shop;

import io.avaje.jsonb.Json;

@Json
public record OrderView(String id, String sku, int qty, long totalCents, String email, String status) {
}
