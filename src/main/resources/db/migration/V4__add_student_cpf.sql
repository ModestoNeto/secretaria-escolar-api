-- CPF do aluno: só os 11 dígitos, sem máscara (a máscara existe apenas na saída da API).
-- Nulo permitido porque a migração roda sobre alunos que já existiam sem CPF; o UNIQUE aceita vários nulos,
-- e a API exige o CPF em todo cadastro e edição novos.
ALTER TABLE students
    ADD COLUMN cpf VARCHAR(11) NULL,
    ADD CONSTRAINT uk_students_cpf UNIQUE (cpf);

-- CPFs fictícios (dígitos verificadores válidos, gerados por algoritmo) para os 12 alunos de exemplo do V2.
-- Por serem válidos, podem coincidir por acaso com CPFs reais: use só para desenvolvimento.
UPDATE students SET cpf = '10000001090' WHERE id = 1;
UPDATE students SET cpf = '10791913309' WHERE id = 2;
UPDATE students SET cpf = '11583825614' WHERE id = 3;
UPDATE students SET cpf = '12375737938' WHERE id = 4;
UPDATE students SET cpf = '13167650249' WHERE id = 5;
UPDATE students SET cpf = '13959562519' WHERE id = 6;
UPDATE students SET cpf = '14751474812' WHERE id = 7;
UPDATE students SET cpf = '15543387199' WHERE id = 8;
UPDATE students SET cpf = '16335299402' WHERE id = 9;
UPDATE students SET cpf = '17127211787' WHERE id = 10;
UPDATE students SET cpf = '17919124000' WHERE id = 11;
UPDATE students SET cpf = '18711036303' WHERE id = 12;
