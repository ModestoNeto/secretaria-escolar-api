-- Avaliação de uma disciplina em uma turma (o vínculo class_subjects diz quem é o professor titular).
-- weight e max_score são DECIMAL: nota e peso decimais não podem ter erro de ponto flutuante.
CREATE TABLE assessments (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    class_subject_id BIGINT       NOT NULL,
    name             VARCHAR(80)  NOT NULL,
    term             INT          NOT NULL,
    weight           DECIMAL(5,2) NOT NULL,
    max_score        DECIMAL(5,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_assessments_subject_term_name UNIQUE (class_subject_id, term, name),
    CONSTRAINT fk_assessments_class_subject FOREIGN KEY (class_subject_id) REFERENCES class_subjects (id),
    CONSTRAINT ck_assessments_term CHECK (term BETWEEN 1 AND 4),
    CONSTRAINT ck_assessments_weight CHECK (weight > 0),
    CONSTRAINT ck_assessments_max_score CHECK (max_score > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Nota de uma matrícula em uma avaliação. Aponta para a matrícula (não para o aluno): se a matrícula for
-- cancelada, a nota continua no histórico. "Nota <= max_score" depende de outra tabela, então é checada no serviço.
CREATE TABLE grades (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    assessment_id BIGINT       NOT NULL,
    enrollment_id BIGINT       NOT NULL,
    score         DECIMAL(5,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_grades_assessment_enrollment UNIQUE (assessment_id, enrollment_id),
    CONSTRAINT fk_grades_assessment FOREIGN KEY (assessment_id) REFERENCES assessments (id),
    CONSTRAINT fk_grades_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id),
    CONSTRAINT ck_grades_score CHECK (score >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
