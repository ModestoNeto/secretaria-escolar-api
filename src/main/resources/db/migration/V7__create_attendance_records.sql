-- Presença de uma matrícula em uma aula (disciplina da turma + data). Uma linha por aluno e aula: o total de aulas
-- do aluno é a contagem das linhas dele. Aponta para a matrícula (não para o aluno), como as notas.
-- O UNIQUE impede registrar duas vezes o mesmo aluno na mesma aula (e é a última barreira contra gravação simultânea).
CREATE TABLE attendance_records (
    id               BIGINT  NOT NULL AUTO_INCREMENT,
    class_subject_id BIGINT  NOT NULL,
    lesson_date      DATE    NOT NULL,
    enrollment_id    BIGINT  NOT NULL,
    present          BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_attendance_lesson_enrollment UNIQUE (class_subject_id, lesson_date, enrollment_id),
    CONSTRAINT fk_attendance_class_subject FOREIGN KEY (class_subject_id) REFERENCES class_subjects (id),
    CONSTRAINT fk_attendance_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Consulta da frequência de uma matrícula por disciplina.
CREATE INDEX ix_attendance_enrollment ON attendance_records (enrollment_id);
