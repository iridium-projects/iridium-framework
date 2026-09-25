package cc.asylum.quarkus.demo.shop;

public record Product(String sku, String name, long cents, int stock) {
}
