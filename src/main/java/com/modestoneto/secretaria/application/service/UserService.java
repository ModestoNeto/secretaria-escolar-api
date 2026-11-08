package com.modestoneto.secretaria.application.service;

import com.modestoneto.secretaria.application.port.in.UserUseCase;
import com.modestoneto.secretaria.application.port.out.UserRepositoryPort;
import com.modestoneto.secretaria.domain.exception.NotFoundException;
import com.modestoneto.secretaria.domain.model.Role;
import com.modestoneto.secretaria.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService implements UserUseCase {

    private final UserRepositoryPort users;

    public UserService(UserRepositoryPort users) {
        this.users = users;
    }

    @Override
    public User create(CreateUserCommand command) {
        return users.save(new User(null, command.name(), command.email(),
                command.passwordHash(), command.role(), true));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> listActiveByRole(Role role) {
        return users.findActiveByRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + id));
    }
}
