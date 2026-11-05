package com.modestoneto.secretaria.adapter.out.security;

import com.modestoneto.secretaria.application.port.out.TokenPort;
import com.modestoneto.secretaria.domain.exception.UnauthorizedException;
import com.modestoneto.secretaria.domain.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
class JwtTokenAdapter implements TokenPort {

    private static final String TYPE = "type";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    JwtTokenAdapter(JwtEncoder encoder, JwtDecoder decoder,
                    @Value("${app.jwt.access-minutes}") long accessMinutes,
                    @Value("${app.jwt.refresh-days}") long refreshDays) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.accessTtl = Duration.ofMinutes(accessMinutes);
        this.refreshTtl = Duration.ofDays(refreshDays);
    }

    @Override
    public String issueAccess(User user) {
        return issue(user, ACCESS, accessTtl);
    }

    @Override
    public String issueRefresh(User user) {
        return issue(user, REFRESH, refreshTtl);
    }

    @Override
    public Long userIdFromRefresh(String refreshToken) {
        try {
            Jwt jwt = decoder.decode(refreshToken);
            if (!REFRESH.equals(jwt.getClaimAsString(TYPE))) {
                throw new UnauthorizedException("Token inválido");
            }
            return Long.valueOf(jwt.getSubject());
        } catch (JwtException e) {
            throw new UnauthorizedException("Token inválido");
        }
    }

    private String issue(User user, String type, Duration ttl) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(user.id()))
                .claim("role", user.role().name())
                .claim(TYPE, type)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
