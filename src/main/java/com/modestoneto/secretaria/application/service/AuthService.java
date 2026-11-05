package com.modestoneto.secretaria.application.service;

import com.modestoneto.secretaria.application.port.in.AuthUseCase;
import com.modestoneto.secretaria.application.port.out.PasswordHasherPort;
import com.modestoneto.secretaria.application.port.out.TokenPort;
import com.modestoneto.secretaria.application.port.out.UserRepositoryPort;
import com.modestoneto.secretaria.domain.exception.UnauthorizedException;
import com.modestoneto.secretaria.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements AuthUseCase {

    private static final String INVALID = "Credenciais inválidas";

    private final UserRepositoryPort users;
    private final PasswordHasherPort hasher;
    private final TokenPort tokens;

    public AuthService(UserRepositoryPort users, PasswordHasherPort hasher, TokenPort tokens) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
    }

    @Override
    @Transactional(readOnly = true)
    public Tokens login(String email, String password) {
        // mesma mensagem para e-mail inexistente, senha errada e usuário inativo
        User user = users.findByEmail(email)
                .filter(User::active)
                .filter(u -> hasher.matches(password, u.passwordHash()))
                .orElseThrow(() -> new UnauthorizedException(INVALID));
        return issue(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Tokens refresh(String refreshToken) {
        User user = users.findById(tokens.userIdFromRefresh(refreshToken))
                .filter(User::active)
                .orElseThrow(() -> new UnauthorizedException(INVALID));
        return issue(user);
    }

    private Tokens issue(User user) {
        return new Tokens(tokens.issueAccess(user), tokens.issueRefresh(user));
    }
}
