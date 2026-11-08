package com.modestoneto.secretaria.application.port.in;

import com.modestoneto.secretaria.domain.model.Role;
import com.modestoneto.secretaria.domain.model.User;

import java.util.List;

public interface UserUseCase {

    /** A senha chega já com hash: quem recebe a senha em texto (Dia 3) é que aplica o BCrypt. */
    record CreateUserCommand(String name, String email, String passwordHash, Role role) {
    }

    User create(CreateUserCommand command);

    User findById(Long id);

    List<User> listActiveByRole(Role role);
}
