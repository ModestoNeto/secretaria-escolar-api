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

## Testes

`./mvnw test` precisa do Docker aberto (não precisa do Compose nem do `.env`).
Os testes de banco sobem um MySQL 8.0 descartável com Testcontainers, um só para toda a suíte.
`ArchitectureTest` (ArchUnit) garante que o `domain` não usa frameworks, que a `application`
não usa Spring Data/JPA e que o núcleo não depende dos adaptadores.
