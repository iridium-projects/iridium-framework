package dev.yyuh.iridium.test.service;

import de.yyuh.iridium.core.component.Component;
import de.yyuh.iridium.core.inject.Inject;
import dev.yyuh.iridium.test.dto.CreateUserRequest;
import dev.yyuh.iridium.test.dto.User;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Component
public final class UserService {

    private final UserRepository repository;
    private final AtomicLong idGenerator = new AtomicLong();

    @Inject
    public UserService(final UserRepository repository) {
        this.repository = repository;
    }

    public List<User> list(final int limit) {
        return repository.findAll().stream().limit(limit).toList();
    }

    public Optional<User> get(final long id) {
        return repository.findById(id);
    }

    public User create(final CreateUserRequest request) {
        final User user = new User(idGenerator.incrementAndGet(), request.name(), request.email());
        return repository.save(user);
    }

    public Optional<User> update(final long id, final CreateUserRequest request) {
        if (repository.findById(id).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(repository.save(new User(id, request.name(), request.email())));
    }

    public Optional<User> updateEmail(final long id, final String email) {
        return repository.findById(id)
                .map(existing -> repository.save(new User(existing.id(), existing.name(), email)));
    }

    public boolean delete(final long id) {
        return repository.deleteById(id);
    }
}
