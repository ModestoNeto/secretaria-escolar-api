package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.application.port.in.UserUseCase;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
class UserController {

    private final UserUseCase users;

    UserController(UserUseCase users) {
        this.users = users;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    UserResponse findById(@PathVariable Long id) {
        return UserResponse.from(users.findById(id));
    }
}
