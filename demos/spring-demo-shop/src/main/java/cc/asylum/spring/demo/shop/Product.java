package cc.asylum.spring.demo.shop;

public record Product(String sku, String name, long cents, int stock) {
}
