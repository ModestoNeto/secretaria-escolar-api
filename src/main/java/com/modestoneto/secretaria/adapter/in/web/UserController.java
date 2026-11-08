package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.application.port.in.UserUseCase;
import com.modestoneto.secretaria.domain.model.Role;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
class UserController {

    private final UserUseCase users;

    UserController(UserUseCase users) {
        this.users = users;
    }

    // `role` é obrigatório: a SECRETARIA não deve listar todos os usuários (nem administradores).
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIA')")
    List<UserResponse> listByRole(@RequestParam Role role) {
        return users.listActiveByRole(role).stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    UserResponse findById(@PathVariable Long id) {
        return UserResponse.from(users.findById(id));
    }
}
