# payment-service — Pagamento PIX & Orquestração SAGA

Microsserviço **Core** do ecossistema de pagamentos PIX do desafio final (módulo
[BE-JV-010] Arquitetura de Software e Ágil II — ver `desafio.md`). Recebe uma requisição de
pagamento PIX e orquestra, via padrão SAGA, a geração do comprovante junto ao microsserviço de
Comprovantes (dependência externa, simulada aqui via WireMock).

**Prazo de entrega: 15/07/2026.**

## Documentação

- `desafio.md` — enunciado completo do desafio (todos os 3 microsserviços) e o escopo exato
  deste repositório.
- `.specify/memory/constitution.md` — princípios de arquitetura e engenharia deste serviço.
- `docs/architecture.md` — visão geral da arquitetura, decisões de design e replicação em
  cloud.
- `docs/regras-de-negocio-e-codigo.md` — **referência rápida de todas as regras de negócio e
  de código**, com apontamento de onde cada uma vive no código. Consulte antes de alterar
  qualquer comportamento.
- `docs/integracao-com-outros-servicos.md` — **checklist de integração** com os serviços reais
  de Comprovantes/Notificação, quando estiverem prontos (troca de config, verificação PACT,
  riscos conhecidos de contrato).
- `specs/001-pix-payment-saga/` — spec, plano técnico, modelo de dados, contratos e tarefas
  (metodologia spec-driven, via GitHub Spec Kit).

## Stack

Java 21 · Spring Boot 3.5 · Spring Data JPA (H2) · Spring WebClient · Resilience4j ·
ArchUnit · PACT (`pact-jvm`) · WireMock

## Rodando localmente

Pré-requisitos: JDK 21+, Docker (para o WireMock).

```bash
docker compose up -d wiremock
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. Console H2 em `http://localhost:8080/h2-console`
(JDBC URL: `jdbc:h2:mem:payment-service`). Swagger UI em
`http://localhost:8080/swagger-ui.html`.

Cenários de exemplo (`curl`) em `specs/001-pix-payment-saga/quickstart.md`.

## Testes

```bash
./mvnw test
```

59 testes cobrindo domínio, saga, persistência (H2, incluindo corrida de concorrência),
cliente HTTP (WireMock), controller (`@WebMvcTest`), arquitetura (ArchUnit), contrato (PACT) e
um smoke test de contexto completo.
Relatório de cobertura (Jacoco) em `target/site/jacoco/index.html` após `./mvnw test`.

## Subir tudo via Docker

```bash
docker compose up --build
```

Sobe `payment-service` (porta 8080) + WireMock simulando o Comprovantes (porta 8081).
