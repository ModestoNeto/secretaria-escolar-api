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
