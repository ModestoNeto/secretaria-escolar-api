package com.modestoneto.secretaria.application.service;

import com.modestoneto.secretaria.domain.exception.TooManyAttemptsException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de tentativas de login por e-mail: 5 falhas em 15 min bloqueiam o e-mail por 15 min.
 * shortcut: memória de uma instância só (some ao reiniciar) e sem limite por IP; com várias
 * instâncias ou ataque com muitos e-mails, mover para Redis/banco e limitar também por IP.
 */
@Component
class LoginAttempts {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int PURGE_ABOVE = 10_000;

    private record State(int failures, Instant windowStart, Instant blockedUntil) {
    }

    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();
    private final Clock clock;

    LoginAttempts(Clock clock) {
        this.clock = clock;
    }

    void checkAllowed(String key) {
        State s = states.get(key);
        Instant now = clock.instant();
        if (s != null && s.blockedUntil() != null && now.isBefore(s.blockedUntil())) {
            throw new TooManyAttemptsException(Math.max(1, Duration.between(now, s.blockedUntil()).toSeconds()));
        }
    }

    void recordFailure(String key) {
        Instant now = clock.instant();
        if (states.size() > PURGE_ABOVE) {
            states.values().removeIf(s -> now.isAfter(s.windowStart().plus(WINDOW)));
        }
        states.compute(key, (k, s) -> {
            if (s == null || now.isAfter(s.windowStart().plus(WINDOW))) {
                return new State(1, now, null);
            }
            int failures = s.failures() + 1;
            return new State(failures, s.windowStart(), failures >= MAX_FAILURES ? now.plus(WINDOW) : null);
        });
    }

    void recordSuccess(String key) {
        states.remove(key);
    }
}
