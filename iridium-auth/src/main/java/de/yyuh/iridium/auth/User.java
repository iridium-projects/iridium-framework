package de.yyuh.iridium.auth;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import de.yyuh.celery.api.entity.IEntity;
import de.yyuh.celery.api.annotation.Identifier;

@NullMarked
public record User(
    @Identifier UUID id,
    String username,
    String email,
    String passwordHash,
    String role) implements IEntity {
}
