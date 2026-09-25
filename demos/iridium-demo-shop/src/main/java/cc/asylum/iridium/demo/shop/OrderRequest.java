package cc.asylum.iridium.demo.shop;

import cc.asylum.iridium.core.validation.annotation.Email;
import cc.asylum.iridium.core.validation.annotation.Min;
import cc.asylum.iridium.core.validation.annotation.NotBlank;
import io.avaje.jsonb.Json;

@Json
public record OrderRequest(
    @NotBlank String sku,
    @Min(1) int qty,
    @Email String email) {
}
