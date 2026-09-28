package cc.asylum.iridium.demo.hello;

import cc.asylum.iridium.core.validation.annotation.Email;
import cc.asylum.iridium.core.validation.annotation.EscapeHtml;
import cc.asylum.iridium.core.validation.annotation.NotBlank;
import io.avaje.jsonb.Json;

@Json
public record HelloDto(
    @EscapeHtml @NotBlank String test,
    @Email String test2) {
}
