package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.application.port.in.AuthUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
class AuthController {

    private final AuthUseCase auth;

    AuthController(AuthUseCase auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return toResponse(auth.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return toResponse(auth.refresh(request.refreshToken()));
    }

    // Sem estado: o logout é do cliente, que descarta os tokens.
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout() {
    }

    private static TokenResponse toResponse(AuthUseCase.Tokens tokens) {
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken());
    }
}
