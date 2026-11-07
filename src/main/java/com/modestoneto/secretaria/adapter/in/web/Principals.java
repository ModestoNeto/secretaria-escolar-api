package com.modestoneto.secretaria.adapter.in.web;

import org.springframework.security.oauth2.jwt.Jwt;

/** Lê o usuário logado do JWT: o `sub` é o id e a claim `role` é o papel. */
final class Principals {

    private Principals() {
    }

    static boolean isProfessor(Jwt jwt) {
        return "PROFESSOR".equals(jwt.getClaimAsString("role"));
    }

    static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
