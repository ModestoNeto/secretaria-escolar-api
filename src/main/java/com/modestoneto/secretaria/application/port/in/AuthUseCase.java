package com.modestoneto.secretaria.application.port.in;

public interface AuthUseCase {

    record Tokens(String accessToken, String refreshToken) {
    }

    Tokens login(String email, String password);

    Tokens refresh(String refreshToken);
}
