# Arquitetura — Payment Service (Core & SAGA)

**Versão**: 1.0.0 | **Data**: 2026-07-08 | **Prazo de entrega**: 2026-07-15

---

## Visão Geral

Este repositório implementa o microsserviço **Pagamento/Fatura** de um ecossistema de 3
microsserviços de pagamento PIX (ver `desafio.md`). É o orquestrador SAGA: garante que uma
fatura só é dada como paga se o comprovante correspondente for confirmado como persistido no
microsserviço de Comprovantes — dependência externa, implementada por outro integrante do
grupo e simulada aqui via **WireMock**.

```
┌───────────────────────────────────────────────────────────────────┐
│                         payment-service (este repo)                │
│                                                                     │
│  POST /api/v1/pagamentos          ┌─────────────────────────────┐  │
│  ───────────────────────────────► │  PagamentoSagaOrchestrator   │  │
│  GET  /api/v1/pagamentos/{id}     │  (application/saga)          │  │
│  ───────────────────────────────► └──────────────┬────────────────┘  │
│                                                  │ WebClient +       │
│                                                  │ Resilience4j      │
└──────────────────────────────────────────────────┼────────────────┘
                                                     ▼
                                    ┌───────────────────────────────┐
                                    │   Comprovantes (WireMock)      │
                                    │   POST /comprovantes  → 202    │
                                    │   GET  /comprovantes/{id}      │
                                    └───────────────────────────────┘
```

O microsserviço de Notificação (Kafka) não é chamado por este serviço — é o Comprovantes quem
publica o evento de sucesso, conforme `desafio.md`.

## Escopo: O que é implementado aqui

Apenas `payment-service`. Comprovantes e Notificação são de outros integrantes do grupo e,
aqui, existem só como contrato (`specs/001-pix-payment-saga/contracts/comprovantes-consumido.md`)
simulado via WireMock (`wiremock/mappings/`) e verificado via PACT
(`target/pacts/payment-service-comprovantes-service.json`).

## Clean Architecture

```
domain/          — Fatura (agregado), Value Objects, eventos, portas. Zero dependência externa.
application/     — PagamentoSagaOrchestrator, casos de uso. Depende só de domain.
infrastructure/  — controller HTTP, cliente WebClient do Comprovantes, persistência JPA,
                   fiação Spring (ApplicationBeansConfig). Depende de application e domain.
```

Isolamento de camadas garantido por `ArchitectureTest` (ArchUnit), rodando em todo `mvn test`.

## Decisão de Design: SAGA síncrona dentro do request HTTP

`POST /api/v1/pagamentos` bloqueia a thread da requisição até a saga chegar a um estado
terminal (`PAGA`/`FALHOU`) ou esgotar a janela de confirmação (`comprovantes.confirmacao.*`
em `application.properties`, padrão: 5 tentativas × 500ms). Isso significa:

- **Vantagem**: sem necessidade de thread pool/fila própria para a saga — mais simples de
  implementar e entender em uma semana, e mais fácil de demonstrar (o cliente já recebe o
  status final na resposta do POST).
- **Custo**: a chamada pode levar até ~2,5s no pior caso (Comprovantes nunca confirma). Aceito
  porque o enunciado não define um SLA de latência, e o circuit breaker limita o pior caso de
  indisponibilidade total do Comprovantes.

Ver `specs/001-pix-payment-saga/research.md` Decisão 1 para o raciocínio completo sobre por
que a confirmação é feita por polling (o contrato do Comprovantes não oferece um callback).

## Resiliência

Toda chamada ao Comprovantes passa por Resilience4j:
- `@CircuitBreaker(name = "comprovantes")` em `solicitar()` e `confirmarPersistencia()`.
- `@Retry(name = "comprovantes")` apenas em `solicitar()` — a confirmação já tem seu próprio
  loop limitado no orquestrador (evita retry duplicado/multiplicativo).
- Configuração em `application.properties`, prefixo `resilience4j.*`.

## Testes

| Camada | Estratégia | Ferramenta |
|---|---|---|
| Domain | Unitário puro | JUnit 5 + AssertJ |
| Application | Unitário com fakes das portas | JUnit 5 + AssertJ |
| Persistência | Integração com H2 real | `@DataJpaTest` |
| Cliente HTTP do Comprovantes | Integração com WireMock real (sem Docker) | `WireMockExtension` |
| Controller | Fatiado, use cases mockados | `@WebMvcTest` + `@MockitoBean` |
| Arquitetura | Regras de camada | ArchUnit |
| Contrato com Comprovantes | Consumidor | PACT (`pact-jvm`) |
| Fumaça | Contexto Spring completo | `@SpringBootTest` |

Cobertura de linha em `domain`+`application`: **98,3%** (exigido pela constituição: ≥80%).

## Replicação em Cloud (Critério de Avaliação #5)

> Nota: este é um requisito do grupo como um todo (aplica-se ao ecossistema completo). Do
> ponto de vista específico do `payment-service`:

- **Stateless**: o serviço não guarda estado em memória entre requisições (idempotência e
  estado da saga vivem no banco) — múltiplas réplicas atrás de um load balancer funcionam sem
  sticky sessions.
- **Banco de dados**: H2 em memória é adequado só para dev/demo; uma implantação replicada
  exigiria trocar para um PostgreSQL gerenciado (ex.: RDS/Cloud SQL) compartilhado entre
  réplicas — troca de configuração apenas, a lógica de domínio não muda (ver constitution.md,
  Stack & Decisões Tecnológicas).
- **Comprovantes/Notificação**: em produção, a URL do WireMock em `comprovantes.base-url` é
  substituída pelo endereço real do serviço de Comprovantes (via variável de ambiente/config
  server) — nenhuma mudança de código.
- **Resiliência entre réplicas**: o circuit breaker é local a cada instância (sem estado
  compartilhado) — aceitável nessa escala; uma evolução futura poderia centralizar métricas de
  circuit breaker via um sidecar/service mesh (ex.: Istio) para decisões consistentes entre
  réplicas.
- **Deploy sugerido**: containers (`Dockerfile` já neste repo) orquestrados via Kubernetes ou
  serviço gerenciado (ECS/Cloud Run), com autoscaling horizontal baseado em CPU/latência.

## ADRs

Nenhuma complexidade além do que os princípios da constituição exigem foi introduzida até
agora — sem ADRs pendentes.
