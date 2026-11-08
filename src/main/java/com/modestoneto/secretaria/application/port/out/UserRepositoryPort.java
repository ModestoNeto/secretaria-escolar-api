package com.modestoneto.secretaria.application.port.out;

import com.modestoneto.secretaria.domain.model.Role;
import com.modestoneto.secretaria.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {

    /** Lança ConflictException se o e-mail já existir. */
    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    /** Só usuários ativos do papel, ordenados por nome. */
    List<User> findActiveByRole(Role role);
}
