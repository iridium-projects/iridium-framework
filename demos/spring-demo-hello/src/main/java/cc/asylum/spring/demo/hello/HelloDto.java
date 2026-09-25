package cc.asylum.spring.demo.hello;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record HelloDto(
    @NotBlank String test,
    @Email String test2) {
}
