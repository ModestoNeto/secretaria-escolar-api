package com.modestoneto.secretaria.adapter.out.persistence;

import com.modestoneto.secretaria.domain.model.User;

final class UserMapper {

    private UserMapper() {
    }

    static User toDomain(UserJpaEntity e) {
        return new User(e.getId(), e.getName(), e.getEmail(), e.getPasswordHash(), e.getRole(), e.isActive());
    }

    static UserJpaEntity toEntity(User u) {
        return new UserJpaEntity(u.id(), u.name(), u.email(), u.passwordHash(), u.role(), u.active());
    }
}
