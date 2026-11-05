package com.modestoneto.secretaria;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;

public abstract class AbstractMySqlIntegrationTest {

    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci");

    static {
        MYSQL.start(); // contêiner único para todos os testes de banco
    }

    // Segredo só de teste: a suíte não depende do .env (nem do CI ter segredos).
    @DynamicPropertySource
    static void jwt(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> "segredo-somente-para-testes-0123456789");
    }
}
