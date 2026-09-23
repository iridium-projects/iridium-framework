package cc.asylum.iridium.test.service;

import cc.asylum.iridium.core.component.Component;
import cc.asylum.iridium.test.dto.User;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public final class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> users = new LinkedHashMap<>();

    @Override
    public List<User> findAll() {
        return List.copyOf(users.values());
    }

    @Override
    public Optional<User> findById(final long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public User save(final User user) {
        users.put(user.id(), user);
        return user;
    }

    @Override
    public boolean deleteById(final long id) {
        return users.remove(id) != null;
    }
}
