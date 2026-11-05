package com.modestoneto.secretaria.application.port.out;

import com.modestoneto.secretaria.domain.model.User;

public interface TokenPort {

    String issueAccess(User user);

    String issueRefresh(User user);

    /** Lança UnauthorizedException se o token for inválido, expirado ou não for de refresh. */
    Long userIdFromRefresh(String refreshToken);
}
