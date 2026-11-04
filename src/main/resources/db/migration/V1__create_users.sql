CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'SECRETARIA', 'PROFESSOR'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Seed fictício (senha só para desenvolvimento, guardada em hash BCrypt)
INSERT INTO users (name, email, password_hash, role, active)
VALUES ('Administrador', 'admin@secretaria.example',
        '$2a$10$gQU/VlCh7Zg8rrVSH/Vnau2PQCulm5BYwC5WpvWsdLqsRzPRcxVRS', 'ADMIN', TRUE);
