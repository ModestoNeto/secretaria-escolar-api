package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.domain.model.Role;
import com.modestoneto.secretaria.domain.model.User;

// Nunca expõe o hash da senha.
record UserResponse(Long id, String name, String email, Role role, boolean active) {

    static UserResponse from(User user) {
        return new UserResponse(user.id(), user.name(), user.email(), user.role(), user.active());
    }
}
