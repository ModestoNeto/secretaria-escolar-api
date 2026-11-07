CREATE TABLE subjects (
    id   BIGINT      NOT NULL AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_subjects_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE school_classes (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    name        VARCHAR(60) NOT NULL,
    grade_level INT         NOT NULL,
    shift       VARCHAR(10) NOT NULL,
    school_year INT         NOT NULL,
    capacity    INT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_classes_name_year UNIQUE (name, school_year),
    CONSTRAINT ck_classes_grade CHECK (grade_level BETWEEN 1 AND 12),
    CONSTRAINT ck_classes_shift CHECK (shift IN ('MORNING', 'AFTERNOON', 'EVENING')),
    CONSTRAINT ck_classes_capacity CHECK (capacity BETWEEN 1 AND 60)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Quem ensina o quê em cada turma. O professor é um usuário de papel PROFESSOR (sem tabela própria).
-- A tabela nasce aqui para a migração não ser editada depois; o uso vem com o vínculo.
CREATE TABLE class_subjects (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    class_id        BIGINT NOT NULL,
    subject_id      BIGINT NOT NULL,
    teacher_user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_class_subjects UNIQUE (class_id, subject_id),
    CONSTRAINT fk_class_subjects_class FOREIGN KEY (class_id) REFERENCES school_classes (id),
    CONSTRAINT fk_class_subjects_subject FOREIGN KEY (subject_id) REFERENCES subjects (id),
    CONSTRAINT fk_class_subjects_teacher FOREIGN KEY (teacher_user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Dados fictícios de exemplo.
INSERT INTO subjects (name) VALUES
('Matemática'), ('Língua Portuguesa'), ('Ciências'), ('História'), ('Geografia');

INSERT INTO school_classes (name, grade_level, shift, school_year, capacity) VALUES
('5º A', 5, 'MORNING', 2026, 30),
('5º B', 5, 'AFTERNOON', 2026, 30);

-- Usuários fictícios para testar os papéis: reaproveitam o hash do administrador (mesma senha de
-- desenvolvimento), então nenhum segredo novo entra no repositório.
INSERT INTO users (name, email, password_hash, role, active)
SELECT 'Secretaria Exemplo', 'secretaria@secretaria.example', password_hash, 'SECRETARIA', TRUE
FROM users WHERE email = 'admin@secretaria.example';

INSERT INTO users (name, email, password_hash, role, active)
SELECT 'Professor Exemplo', 'professor@secretaria.example', password_hash, 'PROFESSOR', TRUE
FROM users WHERE email = 'admin@secretaria.example';
