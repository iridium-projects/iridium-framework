package cc.asylum.spring.demo.shop;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrderRequest(
    @NotBlank String sku,
    @Min(1) int qty,
    @Email String email) {
}
