# Regras de Negócio e Regras de Código — payment-service

Documento de referência única para consultar/alterar regras sem precisar reler o código
inteiro. Cada regra tem: **o que é**, **onde vive** (arquivo:linha), e **o que mais mexe junto**
se você mudar. Fonte da verdade formal continua sendo `desafio.md` (negócio) e
`.specify/memory/constitution.md` (arquitetura) — este arquivo é o atalho prático para os dois.

**Como manter isto atualizado**: toda vez que uma regra abaixo mudar no código, atualize a
entrada correspondente aqui na mesma tarefa. Se uma regra nova for adicionada, adicione uma
entrada nova na seção certa. Não deixe este arquivo divergir do código — um arquivo de regras
desatualizado é pior que nenhum.

---

## 1. Regras de Negócio

### 1.1 Máquina de estados da Fatura

| De | Para | Quando | Onde |
|---|---|---|---|
| — | `RECEBIDA` | Requisição PIX válida aceita | `Fatura.receber()` — `domain/model/Fatura.java:59` |
| `RECEBIDA` | `COMPROVANTE_SOLICITADO` | Comprovantes aceitou o POST (202 + UUID) | `Fatura.solicitarComprovante()` — `Fatura.java:92` |
| `RECEBIDA` | `FALHOU` | Comprovantes rejeitou o POST / circuit breaker aberto | `Fatura.falhar()` — `Fatura.java:115`, chamado por `PagamentoSagaOrchestrator.passoUm_solicitarComprovante()` — `application/saga/PagamentoSagaOrchestrator.java:47` |
| `COMPROVANTE_SOLICITADO` | `PAGA` | GET no Comprovantes confirma persistência | `Fatura.confirmarPagamento()` — `Fatura.java:107` |
| `COMPROVANTE_SOLICITADO` | `FALHOU` | Tentativas de confirmação esgotadas | `Fatura.falhar()`, chamado por `PagamentoSagaOrchestrator.passoDois_confirmarPersistencia()` — `PagamentoSagaOrchestrator.java:64` |

**Qualquer outra transição lança `TransicaoInvalidaException`** (`Fatura.exigirStatus()` —
`Fatura.java:129`). Isso é reforçado por 3 invariantes que o código garante sozinho, sem
precisar de validação externa:
- Não dá pra confirmar pagamento sem ter passado por `COMPROVANTE_SOLICITADO` antes.
- `PAGA` sempre tem `comprovanteId` preenchido (é setado no passo anterior, nunca é nulo
  quando chega em `confirmarPagamento()`).
- `FALHOU` sempre tem `motivoFalha` preenchido (`Fatura.java:120` valida isso).

**Para mudar a máquina de estados**: adicionar um estado novo ou uma transição nova exige
mexer em `StatusFatura` (enum, `domain/model/StatusFatura.java`), no método de transição
correspondente em `Fatura.java`, na constituição (`.specify/memory/constitution.md`, seção
"Fluxo da SAGA"), e nos testes de `FaturaTest.java`.

### 1.2 Idempotência

- Toda requisição de pagamento exige o header `Idempotency-Key` — `PagamentoController.solicitar()`,
  `infrastructure/adapter/http/PagamentoController.java:27`.
- Reenviar a mesma chave retorna a Fatura já existente, **sem rodar a saga de novo e sem
  chamar o Comprovantes de novo** — `SolicitarPagamentoPixUseCase.executar()`,
  `application/usecase/SolicitarPagamentoPixUseCase.java:23`.
- A garantia real não é a checagem em memória (que tem uma janela de corrida), é a constraint
  única no banco em `idempotency_key` (`FaturaJpaEntity.java`, `@UniqueConstraint`). Duas
  requisições concorrentes com a mesma chave: a que perde a corrida recebe
  `IdempotencyKeyDuplicadaException` (`domain/exception/IdempotencyKeyDuplicadaException.java`),
  traduzida em `FaturaRepositoryAdapter.salvar()` e tratada em
  `SolicitarPagamentoPixUseCase.criarEProcessar()` recarregando a fatura vencedora —
  `SolicitarPagamentoPixUseCase.java:28`.

**Para mudar**: se decidir trocar a estratégia de idempotência (ex.: expirar chaves antigas),
mexe em `FaturaJpaRepository`/`FaturaRepositoryAdapter` + `SolicitarPagamentoPixUseCase`.

### 1.3 Validação do payload de entrada

Campos obrigatórios e regras — `infrastructure/adapter/http/PagamentoRequest.java`:

| Campo (JSON) | Regra | Anotação/validação |
|---|---|---|
| `nome` | obrigatório | `@NotBlank` |
| `tipo_documento` | obrigatório, deve ser `CPF` ou `CNPJ` | `@NotBlank` + `TipoDocumento.valueOf()` (lança se inválido) |
| `numero_documento` | obrigatório | `@NotBlank` |
| `numero_agencia` / `numero_conta` / `digito_verificador_conta` | obrigatórios | `@NotBlank` cada um |
| `valor_transacao` | obrigatório, > 0 | `@NotNull @Positive` + `ValorTransacao` arredonda pra 2 casas (`domain/model/ValorTransacao.java`) |
| `tipo_chave_pix_destino` | obrigatório, enum válido | `@NotBlank` + `TipoChavePix.valueOf()` |
| `chave_pix_destino` / `nome_cliente_destino` | obrigatórios | `@NotBlank` |
| `identificacao_pix` | **opcional** | sem anotação |
| `data_hora_transacao` | obrigatório, formato `LocalDateTime` (SEM timezone/offset — ver regra 2.2) | `@NotNull` |

Campo ausente/inválido → `400 Bad Request` via `GlobalExceptionHandler`
(`infrastructure/adapter/http/GlobalExceptionHandler.java`), **nenhuma Fatura é criada** e o
Comprovantes nunca é chamado.

**Para mudar**: adicionar/remover campo obrigatório é só mexer nas anotações de
`PagamentoRequest.java` — mas se o campo for usado no domínio, precisa propagar pro Value
Object correspondente em `domain/model/` também (documentoOrigem, contaOrigem, destinoPix).

### 1.4 Confirmação de persistência (polling limitado)

- Depois que o Comprovantes aceita o POST, `payment-service` consulta `GET /comprovantes/{id}`
  repetidamente até confirmar (200) ou esgotar as tentativas.
- **Não existe timeout total separado** — o limite é só `max-tentativas × intervalo-ms`.
  Hoje: `comprovantes.confirmacao.max-tentativas=5` × `intervalo-inicial-ms=500` = até ~2,5s no
  pior caso (config em `src/main/resources/application.properties`).
- Um erro técnico (5xx, timeout de rede) numa tentativa de confirmação **não é fatal
  imediatamente** — conta como "não confirmado nesta tentativa" e o loop continua até esgotar
  (`PagamentoSagaOrchestrator.confirmado()` — `PagamentoSagaOrchestrator.java:89`).
- Ao esgotar, a Fatura vai pra `FALHOU`, mas **retém o `comprovanteId`** pra reconciliação
  manual futura (o comprovante pode ainda ser gravado depois, de forma assíncrona, pelo lado
  do Comprovantes).

**Para mudar o número de tentativas ou o intervalo**: só editar
`application.properties` → `comprovantes.confirmacao.*` (não precisa mexer em código — já é
injetado via `@Value` em `infrastructure/config/ApplicationBeansConfig.java`).

### 1.5 Resposta síncrona do POST (decisão de design importante)

`POST /api/v1/pagamentos` **bloqueia a requisição HTTP até a saga terminar** (até ~2,5s no
pior caso) e já devolve o status final (`PAGA`/`FALHOU`) no `202 Accepted` — não fica
`RECEBIDA` esperando um polling do cliente. Decisão deliberada de simplicidade (ver
`docs/architecture.md`). Se um dia isso precisar virar assíncrono de verdade (thread pool
próprio pra saga), é uma mudança grande — mexe em `SolicitarPagamentoPixUseCase`,
`PagamentoController` e toda a documentação de contrato.

### 1.6 Casos de borda já cobertos (não reabrir sem querer)

- Comprovantes aceita (202) mas nunca grava de verdade → `FALHOU` com `comprovanteId` retido
  (não é falha muda).
- Reenvio sem `Idempotency-Key` → tratado como fatura nova e independente (idempotência só
  vale com o header presente).
- Reenvio com a mesma `Idempotency-Key` enquanto a saga original ainda está rodando → não
  inicia uma segunda saga (ver 1.2, corrida de concorrência).
- `identificador_comprovante` malformado (não-UUID) na resposta do Comprovantes → tratado como
  indisponibilidade da dependência (compensa a saga), **não** como erro de validação do
  cliente que chamou `payment-service` — `ComprovanteHttpClient.solicitar()`,
  `infrastructure/adapter/client/ComprovanteHttpClient.java:31`.

---

## 2. Regras de Código / Arquitetura

### 2.1 Clean Architecture — as 3 camadas

```
domain/           zero dependência externa (nem Spring, nem JPA, nem HTTP)
application/      depende só de domain (nem Spring pode entrar aqui)
infrastructure/   depende de application e domain (é onde Spring/JPA/HTTP vivem)
```

Imposto automaticamente por `ArchitectureTest`
(`src/test/java/com/projetocore/payment/architecture/ArchitectureTest.java`) — roda em todo
`mvn test`. Se uma mudança quebrar isolamento de camada, esse teste falha primeiro.

**Regra prática**: se você precisar importar algo de `org.springframework.*` ou
`jakarta.persistence.*` dentro de `domain/` ou `application/`, pare — a coisa certa a fazer é
criar uma porta (`domain/port/`) e implementar em `infrastructure/`.

### 2.2 Datas: `LocalDateTime` vs `Instant` (regra que já causou um bug real)

- **`dataHoraTransacao`** (dado que o cliente manda, vindo do payload PIX) é sempre
  **`LocalDateTime`**, nunca `Instant`. O payload do desafio não tem timezone/offset
  (`"2022-04-10T20:03:57.116061100"`) — `Instant.parse` rejeita esse formato.
- **`criadaEm`, `atualizadaEm`, `ocorridoEm`, `dataHoraRequisicao`** (timestamps que o
  **próprio servidor** gera, via `Instant.now()`) continuam **`Instant`** — esses sim têm `Z`.
- Qualquer `WebClient` novo que for criado (produção ou teste) **precisa** usar o
  `ObjectMapper` configurado (ver `infrastructure/config/WebClientConfig.java` e, em testes,
  `WebClientTestFactory`) — um `WebClient.builder().build()` "cru" serializa `LocalDateTime`
  como array em vez de string ISO. Detalhes completos em `research.md`, seção "Achados da
  varredura de validação".

**Regra de ouro**: ao adicionar qualquer campo de data/hora novo, pergunte "isso vem do
cliente (sem timezone) ou é gerado por nós (com timezone)?" antes de escolher o tipo.

### 2.3 Resiliência (Resilience4j)

- `solicitar()` (POST no Comprovantes): `@CircuitBreaker` + `@Retry`, nome da instância
  `comprovantes` — `ComprovanteHttpClient.java:31-33`.
- `confirmarPersistencia()` (GET): só `@CircuitBreaker`, **sem** `@Retry` — de propósito, pra
  não multiplicar tentativas (o loop do orquestrador já é o retry desse caso) —
  `ComprovanteHttpClient.java:55`.
- Configuração em `application.properties` → `resilience4j.circuitbreaker.instances.comprovantes` e
  `resilience4j.retry.instances.comprovantes`.

### 2.4 Testes

- TDD: teste antes da implementação, em domain/application primeiro (ver
  `feedback_tasks_inside_out` — organização por camada, não por user story).
- Meta de cobertura: ≥80% em `domain`+`application` (constituição, Princípio III) — hoje em
  **98,4%**. Rodar `./mvnw test` gera relatório em `target/site/jacoco/index.html`.
- Toda mudança de comportamento externo (contrato HTTP com Comprovantes) precisa refletir em
  3 lugares ao mesmo tempo: `wiremock/mappings/*.json`, o teste PACT
  (`src/test/java/.../contract/ComprovanteContractTest.java`) e `contracts/comprovantes-consumido.md`.
- **Regra aprendida**: sempre ter pelo menos um teste com o payload **literal** do `desafio.md`,
  sem nenhum ajuste (nada de adicionar "Z" pra facilitar) — foi assim que os 3 bugs da
  varredura de 2026-07-08 foram achados.

### 2.5 Mapeamento de dependências externas (WireMock)

Cenários simulados via `numero_documento` sentinela no payload (não precisa trocar arquivo de
mapping manualmente):

| `numero_documento` | Simula | Arquivo |
|---|---|---|
| qualquer outro valor | sucesso (202 + confirmação) | `wiremock/mappings/comprovantes-post-202.json` |
| `00000000000` | Comprovantes rejeita o POST (500) | `comprovantes-post-500.json` |
| `11111111111` | POST aceito, mas nunca confirma (GET sempre 404) | `comprovantes-post-202-nunca-confirmado.json` |
| `22222222222` | POST aceito, mas `identificador_comprovante` inválido | `comprovantes-post-202-uuid-invalido.json` |

**Para adicionar um cenário novo**: criar um novo arquivo `wiremock/mappings/*.json` com
`bodyPatterns.matchesJsonPath` num `numero_documento` sentinela novo, `priority` menor que 10
(o catch-all de sucesso tem `priority: 10`).

### 2.6 Onde cada coisa mora (mapa rápido)

```
domain/model/Fatura.java                          agregado + máquina de estados
domain/model/*.java (Valor/Documento/Conta/...)    value objects, cada um com sua validação
domain/port/*.java                                 interfaces (portas) — contrato entre camadas
application/saga/PagamentoSagaOrchestrator.java     lógica da SAGA (os 2 passos)
application/usecase/SolicitarPagamentoPixUseCase    idempotência + aciona a saga
application/usecase/ConsultarFaturaUseCase          consulta por id
infrastructure/adapter/http/PagamentoController      endpoints REST
infrastructure/adapter/client/ComprovanteHttpClient  chamada HTTP real ao Comprovantes
infrastructure/persistence/*                         JPA (entidade, mapper, repositório)
infrastructure/config/ApplicationBeansConfig         fiação dos beans da camada application
infrastructure/config/WebClientConfig                WebClient com Jackson configurado certo
```

### 2.7 Coisas fora de escopo de propósito (não implementar sem alinhar antes)

- Autenticação/autorização — não é exigido pelo enunciado desta entrega.
- RFC 7807 completo nos erros — resposta de erro é um JSON simples, de propósito.
- Publicação em Kafka/RabbitMQ — não é responsabilidade deste serviço (ver `desafio.md`,
  Comprovantes e Notificação são de outros integrantes).
