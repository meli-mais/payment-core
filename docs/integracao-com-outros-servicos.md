# Guia de Integração com os Serviços Reais (Comprovantes / Notificação)

Hoje o `payment-service` fala com um **WireMock** que simula o Comprovantes
(`wiremock/mappings/*.json`). Este guia é o passo a passo de onde mexer quando o serviço real
de Comprovantes (Membro 2/3) estiver pronto. Guarde este arquivo — é ele que eu vou seguir
quando você pedir "agora integra com o Comprovantes de verdade".

**Notificação (Kafka)**: nada a fazer aqui. `payment-service` nunca fala com esse serviço,
nem direto nem indireto — ver `docs/regras-de-negocio-e-codigo.md`, seção 2.7.

---

## Checklist rápido (ordem sugerida)

- [ ] 1. Confirmar a URL real do Comprovantes com o time
- [ ] 2. Trocar `comprovantes.base-url`
- [ ] 3. Conferir se os *paths* batem (`/comprovantes`, `/comprovantes/{id}`)
- [ ] 4. Conferir se precisa de autenticação (hoje não existe nenhuma)
- [ ] 5. Rodar a verificação PACT do lado provider com o time de Comprovantes
- [ ] 6. Reajustar timeouts/resiliência com a latência real (hoje calibrado pro WireMock, que
      responde na hora)
- [ ] 7. Validar manualmente os 3 cenários de `quickstart.md` apontando pro serviço real
- [ ] 8. Decidir o que fazer com o WireMock no `docker-compose.yml` (ver seção 7)

---

## 1. Confirmar a URL real com o time

Pergunte pro Membro 2/3: qual é a URL base do serviço deles (localhost com outra porta? um
container na mesma rede Docker? um ambiente hospedado?). É esse valor que entra no passo 2.

## 2. Trocar `comprovantes.base-url`

Arquivo: `src/main/resources/application.properties`, linha:
```properties
comprovantes.base-url=http://localhost:8081
```
Troque `http://localhost:8081` pela URL real. Se for rodar tudo via Docker Compose na mesma
rede, normalmente é o nome do serviço (ex.: `http://comprovantes-service:8080`), igual ao
`docker-compose.yml` já faz hoje pro WireMock via `COMPROVANTES_BASE_URL` (variável de
ambiente que sobrescreve essa property — veja `docker-compose.yml`).

**Não precisa mexer em código nenhum pra isso** — é só configuração, exatamente como o
projeto foi desenhado (Ports & Adapters: `ComprovanteGatewayPort` no domínio,
`ComprovanteHttpClient` como implementação, URL injetada via `@Value`).

## 3. Conferir se os *paths* batem

`ComprovanteHttpClient.java` chama:
- `POST {base-url}/comprovantes` — linha 37
- `GET {base-url}/comprovantes/{id}` — linha 65

Se o serviço real do Membro 2/3 usar um path diferente (ex.: `/api/v1/comprovantes`, ou algum
prefixo de contexto), é só ajustar essas duas linhas — `infrastructure/adapter/client/ComprovanteHttpClient.java`.

## 4. Autenticação

**Hoje não existe nenhuma autenticação nas chamadas ao Comprovantes** (nem token, nem API
key). Se o serviço real exigir, o lugar certo pra adicionar é
`infrastructure/config/WebClientConfig.java` — um `defaultHeader(...)` ou um
`ExchangeFilterFunction` no builder do `WebClient`, ou uma variável de ambiente com o
token/chave se for autenticação simples. Não é código de domínio nem de aplicação — fica
isolado na infraestrutura, sem tocar em `ComprovanteHttpClient`.

## 5. Verificação PACT (o passo mais importante antes de confiar na integração)

Já existe um contrato gerado como consumidor:
`target/pacts/payment-service-comprovantes-service.json` (gerado a cada `mvn test`, pelo
`ComprovanteContractTest`).

Antes de apontar pro serviço real "sem rede de segurança", peça pro time de Comprovantes
rodar a **verificação do lado provider** (Pact Verifier) contra esse arquivo. Isso confirma
que a implementação real deles bate com o que `payment-service` espera — sem precisar de
integração manual às cegas. Se algo não bater (nome de campo diferente, status code
diferente), aparece nessa verificação, não em produção.

Se o grupo decidir usar um **Pact Broker** compartilhado (opcional, mencionado como próximo
passo em `tasks.md`), esse arquivo é o que se publica lá.

## 6. Reajustar timeouts e resiliência

Hoje o WireMock responde instantaneamente, então os valores em
`src/main/resources/application.properties` foram calibrados pra isso, não pra uma rede/serviço
real:

```properties
comprovantes.confirmacao.max-tentativas=5
comprovantes.confirmacao.intervalo-inicial-ms=500

resilience4j.circuitbreaker.instances.comprovantes.wait-duration-in-open-state=10s
resilience4j.retry.instances.comprovantes.wait-duration=300ms
```

Com o serviço real, meça a latência típica (principalmente do `GET`, que é chamado em loop
até confirmar) e ajuste `max-tentativas`/`intervalo-inicial-ms` pra cobrir o tempo real que o
Comprovantes deles leva pra persistir (lembrando que, do lado deles, isso passa por uma fila
RabbitMQ + consumer — pode não ser instantâneo). Nenhuma dessas mudanças exige recompilar,
são só valores de configuração.

## 7. Validar manualmente

Repetir os 3 cenários de `specs/001-pix-payment-saga/quickstart.md`, mas com
`comprovantes.base-url` apontando pro serviço real em vez do WireMock. Prestar atenção
especial no Cenário 3 (falhas) — os `numero_documento` sentinela (`00000000000`,
`11111111111`, `22222222222`) só funcionam contra o WireMock; pra testar falha contra o
serviço real, é preciso um cenário real deles (ex.: desligar o serviço, ou usar um dado que
realmente cause rejeição do lado deles).

## 8. O que fazer com o WireMock

O WireMock **não desaparece** — ele continua sendo útil para:
- Rodar os testes automatizados (`ComprovanteHttpClientTest`, `ComprovanteContractTest`) sem
  depender do serviço real estar no ar.
- Desenvolvimento local de quem não tem o Comprovantes real rodando na própria máquina.

O `docker-compose.yml` de produção/demo (quando o grupo decidir consolidar os 3 serviços) que
troca — aí sim o serviço `wiremock` pode ser substituído pelo serviço real de Comprovantes no
compose, ou os dois convivem em ambientes diferentes (WireMock só em `test`/dev local, serviço
real em um compose de integração). Isso é decisão de infraestrutura do grupo, não deste
arquivo.

---

## Riscos conhecidos (coisas que talvez não batam de primeira)

Estas são suposições que fiz ao implementar, baseadas só em `desafio.md` (não no código real
do Comprovantes, que não existe ainda neste momento). Se algo não bater na integração real,
comece por aqui:

- **Formato de `data_hora_transacao`**: assumi `LocalDateTime` sem timezone, replicando
  exatamente o exemplo do desafio. Se o serviço real deles esperar outro formato (com
  timezone, epoch millis, etc.), ajustar em `ComprovanteRequest.java` e
  `SolicitacaoComprovante.java` (ver `docs/regras-de-negocio-e-codigo.md`, seção 2.2).
- **Nomes de campos JSON**: segui `desafio.md` ao pé da letra (snake_case:
  `identificador_comprovante`, `data_hora_requisicao`, etc.). Se a implementação real deles
  usar nomes ligeiramente diferentes, ajustar as anotações `@JsonProperty` em
  `ComprovanteRequest.java`/`ComprovanteResponse.java`.
- **Corpo do `GET /comprovantes/{id}`**: hoje `payment-service` só olha o **status HTTP**
  (200 = confirmado, 404 = não confirmado ainda) e ignora o corpo da resposta — ver
  `ComprovanteHttpClient.confirmarPersistencia()`. Se precisar validar algo do corpo também
  (ex.: conferir se o `identificador_comprovante` do GET bate com o que foi solicitado), essa
  lógica precisa ser adicionada ali.
- **Content negotiation**: assumi `application/json` simples. Se o serviço real exigir um
  `Accept`/`Content-Type` diferente ou versionado, ajustar em `WebClientConfig.java` ou nas
  chamadas do `ComprovanteHttpClient`.
