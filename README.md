# Secretaria Escolar: API

API REST de gestão escolar (alunos, turmas, matrículas, notas, frequência e boletim)
com perfis de acesso. Java 21, Spring Boot 4, MySQL 8, arquitetura hexagonal.

> Em construção. Projeto de estudo, apenas com dados fictícios.

## Como rodar

1. Copie `.env.example` para `.env` e defina as senhas.
2. `docker compose up -d` (MySQL na porta 3309).
3. `./mvnw spring-boot:run` (API na porta 3039).
4. Verifique: `curl http://localhost:3039/actuator/health`

## Arquitetura

Hexagonal: `domain` (modelo e exceções, sem frameworks), `application` (portas de entrada e saída
e serviços), `adapter` (persistência e, nos próximos dias, web) e `config`.
O núcleo (`domain` e `application`) não depende dos adaptadores nem de JPA/Spring Data;
as entidades JPA ficam só em `adapter.out.persistence`. A aplicação usa `@Service` e
`@Transactional` por decisão pragmática.

## Autenticação

`POST /auth/login` (e-mail e senha) devolve um JWT de **acesso** (15 min) e um de **refresh** (7 dias),
assinados com HS256 usando o `JWT_SECRET` do `.env` (32 caracteres ou mais). `POST /auth/refresh` troca
o refresh por um novo par e `POST /auth/logout` responde 204.

Decisões:
- **Sem estado:** não há tabela de tokens; o logout é do cliente, que descarta os tokens. Um refresh
  roubado vale até expirar (por isso a vida curta do acesso e o `JWT_SECRET` fora do repositório).
- Cada token leva a claim `type`: o refresh **não autentica** rotas normais e o acesso não serve no
  `/auth/refresh`.
- E-mail inexistente, senha errada e usuário inativo dão o mesmo 401 ("Credenciais inválidas").
- Senhas com BCrypt; o núcleo só conhece as portas `PasswordHasherPort` e `TokenPort`.
- Sem token ou com token inválido a resposta é 401.

## Autorização

O papel (`ADMIN`, `SECRETARIA`, `PROFESSOR`) vai na claim `role` do JWT e vira `ROLE_<papel>`.
As rotas são protegidas por `@PreAuthorize` (por enquanto `GET /users/{id}`, só `ADMIN`);
a matriz completa vem com os módulos. Erros 401 e 403 saem no formato
`StandardError {timestamp, status, error, message, path}`, como os demais. O CORS libera apenas
`http://localhost:4200` (propriedade `app.cors.origins`).

## Alunos

`POST /students`, `GET /students/{id}`, `PUT /students/{id}` e `DELETE /students/{id}`
(`ADMIN` e `SECRETARIA`). A migração `V2` cria a tabela e 12 alunos fictícios de exemplo.

Decisões:
- **Matrícula gerada** no formato ano + id com 6 dígitos (ex.: `2026000013`), com o ano vindo de um
  `Clock` injetável. Ela depende do id, e o MySQL não aceita coluna gerada sobre `AUTO_INCREMENT`;
  por isso o adaptador salva com um valor provisório único e troca pelo definitivo na mesma transação.
  A matrícula e o estado ativo nunca vêm do cliente: a edição preserva os dois.
- **Desativar, não apagar:** o `DELETE` marca o aluno como inativo (idempotente, sempre 204), porque
  matrículas e notas dependem dele.
- **DTOs com validação** (nome, nascimento no passado, telefone só com dígitos, e-mail opcional);
  e-mail vazio (`""`) vira nulo. Erros no formato `StandardError`, inclusive JSON quebrado e id que
  não é número (400).
- **PROFESSOR sem acesso por enquanto:** a regra "só os alunos das suas turmas" depende das turmas
  (próximas fases); até lá o acesso é negado (403).

### Listagem paginada

`GET /students?page=0&size=20&sort=fullName,asc&includeInactive=false` devolve
`{content, page, size, totalElements, totalPages}`.
- **Tipos próprios** (`PageRequest`, `PageResult<T>`, `SortDirection` em `application.common`): a
  `application` não pode importar Spring Data, então o `Page`/`Pageable` do Spring só existem dentro do
  adaptador de persistência (o ArchUnit vigia isso).
- **Ordenação por lista fixa** (`fullName`, `registrationNumber`, `birthDate`): qualquer outro campo,
  direção inválida, `page < 0` ou `size` fora de 1 a 100 devolve 400, nunca 500.
- **Desempate por `id`** em toda ordenação, para a ordem não mudar entre páginas quando há nomes iguais.
- Só alunos ativos por padrão; `includeInactive=true` inclui os desativados.

### Busca por nome

`GET /students?q=jose` filtra por trecho do nome, **sem distinguir acento nem caixa** (`jose`, `JOSÉ`
e `Jose` acham "José"), com até 100 caracteres (mais que isso é 400).
- **Sem normalização no código:** a coluna usa a colação `utf8mb4_0900_ai_ci` (*accent-insensitive*,
  *case-insensitive*), e o `LIKE` do MySQL já compara assim. Se alguém trocar a colação, o teste de
  acento falha.
- O `Containing` do Spring Data **escapa** `%` e `_`: `q=%` procura um `%` literal, não "todos".
- `%q%` não usa índice (varre a tabela); para o porte de uma escola não pesa, e fica registrado como
  ponto de otimização se o volume crescer.

## Turmas e disciplinas

`POST/GET /subjects` e `POST/GET /classes`, `GET/PUT /classes/{id}` (`ADMIN` e `SECRETARIA`; o
`PROFESSOR` só ganha consulta das suas turmas com o vínculo professor-disciplina). A migração `V3`
cria as tabelas e dados fictícios: 5 disciplinas, 2 turmas e os usuários `secretaria@secretaria.example`
e `professor@secretaria.example`, que reaproveitam o hash do administrador (mesma senha de
desenvolvimento, nenhum segredo novo).
- **Turma:** nome (até 60), série de 1 a 12, turno (`MORNING`, `AFTERNOON`, `EVENING`), ano letivo e
  capacidade de 1 a 60. O nome é único **dentro do ano**: a turma "5º A" existe em 2026 e em 2027.
- **Disciplina:** nome único, e a colação `utf8mb4_0900_ai_ci` faz "Matemática" e "matematica" serem a
  mesma disciplina (409).
- **409 Conflict:** o adaptador de persistência traduz a violação de unicidade em `ConflictException`
  e o `GlobalExceptionHandler` a devolve como `StandardError`, com mensagem clara.
- **Sem paginação:** as listas são pequenas (por escola); `GET /classes?schoolYear=2026` filtra por ano.
- **Sem tabela de professor:** o professor é um usuário com papel `PROFESSOR`; o vínculo guarda o id
  dele (`class_subjects`, criada no `V3`).

### Vínculo professor e disciplina

`POST /classes/{id}/subjects` (`{subjectId, teacherUserId}`), `GET /classes/{id}/subjects` e
`DELETE /classes/{id}/subjects/{subjectId}`. Escrever é de `ADMIN` e `SECRETARIA`.
- **Regras:** turma ou disciplina inexistente é 404; o usuário precisa existir, estar ativo e ter o
  papel `PROFESSOR`, senão 400 (a mensagem diz o motivo); a mesma disciplina na mesma turma é 409.
  Desvincular repetido dá 404.
- **Posse do professor** ("consulta das suas"): o `sub` do JWT é o id do usuário. `GET /classes` e
  `GET /classes/{id}` passam a aceitar `PROFESSOR`, mas só devolvem as turmas em que ele tem vínculo
  (a consulta usa `EXISTS`, então uma turma com várias disciplinas dele aparece uma vez). Turma que não
  é dele responde **404, e não 403**, para não revelar que ela existe; em
  `GET /classes/{id}/subjects` ele vê só os vínculos dele. Há teste que quebra se essa checagem sumir.
- A resposta do vínculo traz ids, sem nome da disciplina nem do professor (o front resolve pelas
  listas); embutir os nomes entra se o volume pedir, com consulta própria para evitar N+1.

## Testes

`./mvnw test` precisa do Docker aberto (não precisa do Compose nem do `.env`).
Os testes de banco sobem um MySQL 8.0 descartável com Testcontainers, um só para toda a suíte.
`ArchitectureTest` (ArchUnit) garante que o `domain` não usa frameworks, que a `application`
não usa Spring Data/JPA e que o núcleo não depende dos adaptadores.
