package dev.yyuh.iridium.test.service;

import dev.yyuh.iridium.test.dto.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    List<User> findAll();

    Optional<User> findById(long id);

    User save(User user);

    boolean deleteById(long id);
}
