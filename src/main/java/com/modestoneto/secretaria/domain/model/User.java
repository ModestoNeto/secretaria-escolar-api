package com.modestoneto.secretaria.domain.model;

public record User(Long id, String name, String email, String passwordHash, Role role, boolean active) {
}
