package de.yyuh.iridium.auth;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record User(
    UUID id,
    String username,
    String email,
    String passwordHash,
    String role) {
}
