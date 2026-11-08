package com.modestoneto.secretaria.application.service;

import com.modestoneto.secretaria.application.port.in.AuthUseCase;
import com.modestoneto.secretaria.application.port.out.PasswordHasherPort;
import com.modestoneto.secretaria.application.port.out.TokenPort;
import com.modestoneto.secretaria.application.port.out.UserRepositoryPort;
import com.modestoneto.secretaria.domain.exception.UnauthorizedException;
import com.modestoneto.secretaria.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService implements AuthUseCase {

    private static final String INVALID = "Credenciais inválidas";

    private final UserRepositoryPort users;
    private final PasswordHasherPort hasher;
    private final TokenPort tokens;

    private final LoginAttempts attempts;
    // Hash de uma senha aleatória: gasta o mesmo tempo de BCrypt quando o e-mail não existe.
    private final String dummyHash;

    public AuthService(UserRepositoryPort users, PasswordHasherPort hasher, TokenPort tokens, LoginAttempts attempts) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
        this.attempts = attempts;
        this.dummyHash = hasher.hash(UUID.randomUUID().toString());
    }

    @Override
    @Transactional(readOnly = true)
    public Tokens login(String email, String password) {
        String key = email.toLowerCase(Locale.ROOT);
        attempts.checkAllowed(key);
        // BCrypt sempre roda e a mensagem é a mesma para e-mail inexistente, senha errada e usuário inativo
        Optional<User> found = users.findByEmail(email);
        boolean passwordOk = hasher.matches(password, found.map(User::passwordHash).orElse(dummyHash));
        if (!passwordOk || found.isEmpty() || !found.get().active()) {
            attempts.recordFailure(key);
            throw new UnauthorizedException(INVALID);
        }
        attempts.recordSuccess(key);
        return issue(found.get());
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
