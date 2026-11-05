CREATE TABLE students (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    registration_number VARCHAR(20)  NOT NULL,
    full_name           VARCHAR(160) NOT NULL,
    birth_date          DATE         NOT NULL,
    guardian_name       VARCHAR(160) NOT NULL,
    guardian_phone      VARCHAR(11)  NOT NULL,
    email               VARCHAR(160) NULL,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_students_registration UNIQUE (registration_number)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_students_full_name ON students (full_name);

-- Alunos fictícios de exemplo. A matrícula (provisória no INSERT) recebe o mesmo formato
-- que o adaptador usa: ano + id com 6 dígitos.
INSERT INTO students (registration_number, full_name, birth_date, guardian_name, guardian_phone, email) VALUES
('TMP01', 'Ana Beatriz Souza',    '2014-03-12', 'Carla Souza',      '11987654321', NULL),
('TMP02', 'José Carlos Lima',     '2013-07-25', 'Marcos Lima',      '11976543210', 'jose.lima@exemplo.test'),
('TMP03', 'Maria Eduarda Rocha',  '2015-01-08', 'Paula Rocha',      '21965432109', NULL),
('TMP04', 'João Pedro Almeida',   '2012-11-30', 'Renata Almeida',   '31954321098', 'joao.almeida@exemplo.test'),
('TMP05', 'Lúcia Helena Ferreira','2014-09-17', 'Sérgio Ferreira',  '41943210987', NULL),
('TMP06', 'Inês Cristina Duarte', '2013-05-02', 'Fátima Duarte',    '51932109876', NULL),
('TMP07', 'Ângela Maria Prado',   '2015-12-21', 'Roberto Prado',    '61921098765', 'angela.prado@exemplo.test'),
('TMP08', 'Gabriel Henrique Melo','2012-04-14', 'Cláudia Melo',     '71910987654', NULL),
('TMP09', 'Beatriz Nascimento',   '2014-08-06', 'Vinícius Nascimento','81909876543', NULL),
('TMP10', 'Rafael Augusto Pinto', '2013-02-19', 'Simone Pinto',     '85998765432', 'rafael.pinto@exemplo.test'),
('TMP11', 'Camila Vitória Barros','2015-06-28', 'Eduardo Barros',   '92987654321', NULL),
('TMP12', 'Thiago Moreira Gomes', '2012-10-09', 'Luciana Gomes',    '98976543210', NULL);

UPDATE students SET registration_number = CONCAT('2026', LPAD(id, 6, '0'))
WHERE registration_number LIKE 'TMP%';
