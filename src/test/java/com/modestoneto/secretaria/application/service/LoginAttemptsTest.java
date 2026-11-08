package com.modestoneto.secretaria.application.service;

import com.modestoneto.secretaria.domain.exception.TooManyAttemptsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptsTest {

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-07T12:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginAttempts attempts = new LoginAttempts(clock);

    private void fail(String key, int times) {
        for (int i = 0; i < times; i++) {
            attempts.recordFailure(key);
        }
    }

    @Test
    void quatroFalhasNaoBloqueiam() {
        fail("a", 4);
        assertDoesNotThrow(() -> attempts.checkAllowed("a"));
    }

    @Test
    void quintaFalhaBloqueiaPor15Minutos() {
        fail("a", 5);
        TooManyAttemptsException e = assertThrows(TooManyAttemptsException.class, () -> attempts.checkAllowed("a"));
        assertEquals(900, e.retryAfterSeconds());

        clock.now = clock.now.plus(Duration.ofMinutes(15));
        assertDoesNotThrow(() -> attempts.checkAllowed("a"));
    }

    @Test
    void depoisDoBloqueioContaDoZero() {
        fail("a", 5);
        clock.now = clock.now.plus(Duration.ofMinutes(16));
        fail("a", 4);
        assertDoesNotThrow(() -> attempts.checkAllowed("a"));
    }

    @Test
    void falhasForaDaJanelaNaoSeSomam() {
        fail("a", 4);
        clock.now = clock.now.plus(Duration.ofMinutes(16));
        fail("a", 4);
        assertDoesNotThrow(() -> attempts.checkAllowed("a"));
    }

    @Test
    void sucessoZeraEChavesSaoIndependentes() {
        fail("a", 4);
        attempts.recordSuccess("a");
        fail("a", 4);
        assertDoesNotThrow(() -> attempts.checkAllowed("a"));

        fail("b", 5);
        assertDoesNotThrow(() -> attempts.checkAllowed("a"));
        assertThrows(TooManyAttemptsException.class, () -> attempts.checkAllowed("b"));
    }
}
