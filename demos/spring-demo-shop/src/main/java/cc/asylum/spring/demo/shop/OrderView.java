package cc.asylum.spring.demo.shop;

public record OrderView(String id, String sku, int qty, long totalCents, String email, String status) {
}
