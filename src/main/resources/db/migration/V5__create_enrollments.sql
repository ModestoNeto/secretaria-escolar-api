-- Matrícula do aluno em uma turma. A matrícula cancelada fica no banco (as notas e a frequência
-- dos próximos dias apontam para ela), por isso há status em vez de DELETE físico.
-- school_year repete o ano da turma: a unicidade "uma matrícula ativa por aluno e ano" precisa dele na mesma tabela.
-- active_year só tem valor quando a matrícula está ATIVA; o UNIQUE aceita vários nulos, então um aluno
-- pode ter várias matrículas canceladas no mesmo ano, mas só uma ativa.
CREATE TABLE enrollments (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    student_id  BIGINT      NOT NULL,
    class_id    BIGINT      NOT NULL,
    school_year INT         NOT NULL,
    status      VARCHAR(10) NOT NULL,
    enrolled_at DATETIME(6) NOT NULL,
    active_year INT GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN school_year END) VIRTUAL,
    PRIMARY KEY (id),
    CONSTRAINT uk_enrollments_student_active_year UNIQUE (student_id, active_year),
    CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_enrollments_class FOREIGN KEY (class_id) REFERENCES school_classes (id),
    CONSTRAINT ck_enrollments_status CHECK (status IN ('ACTIVE', 'CANCELLED')),
    INDEX idx_enrollments_class_status (class_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
