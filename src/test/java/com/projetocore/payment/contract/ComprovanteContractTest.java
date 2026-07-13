package com.projetocore.payment.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.projetocore.payment.domain.model.*;
import com.projetocore.payment.domain.port.ComprovanteSolicitado;
import com.projetocore.payment.domain.port.SolicitacaoComprovante;
import com.projetocore.payment.infrastructure.adapter.client.ComprovanteHttpClient;
import com.projetocore.payment.infrastructure.adapter.client.WebClientTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de contrato (requisito 4 do desafio): payment-service como CONSUMIDOR do contrato do
 * microsserviço de Comprovantes. O pacto gerado em {@code target/pacts/} é o artefato que o
 * time de Comprovantes (Membro 2/3) usa para rodar a verificação do lado provider
 * (constitution.md, Princípio V). API do pact-jvm 4.6.x (DSL {@code PactBuilder}).
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "comprovantes-service")
class ComprovanteContractTest {

    private static final String COMPROVANTE_ID = "b819cc65-f6f0-478c-8bb7-69ee1c4f6402";

    /**
     * Timestamp de teste com 9 dígitos fracionários SEM zero à direita, de propósito: o
     * `ObjectMapper` (corretamente) reserializa {@code LocalDateTime} eliminando zeros à
     * direita da fração de segundos (mesmo valor em nanossegundos, representação textual
     * mais curta) — usar aqui um valor como "...116061100" faria o corpo reserializado
     * ("...1160611") não bater byte a byte com o literal deste teste, por um detalhe de
     * formatação irrelevante para o contrato real (nenhum parser JSON/data se importa com
     * zeros à direita). `desafio.md` usa um exemplo com zeros à direita, mas isso é
     * incidental ao exemplo, não uma exigência do contrato.
     */
    private static final String DATA_HORA_TRANSACAO = "2026-07-08T20:03:57.123456789";

    private static final String REQUEST_JSON = """
            {
              "nome": "Giovanni Vicente",
              "tipo_documento": "CPF",
              "numero_documento": "50329291076",
              "numero_agencia": "2022",
              "numero_conta": "00276",
              "digito_verificador_conta": "0",
              "valor_transacao": 23.99,
              "tipo_chave_pix_destino": "CELULAR",
              "chave_pix_destino": "11948755536",
              "nome_cliente_destino": "Fernando Augusto",
              "identificacao_pix": "Segue pagamento da minha cota no churrasco de domingo",
              "data_hora_transacao": "%s"
            }
            """.formatted(DATA_HORA_TRANSACAO);

    private static final String RESPONSE_202_JSON = """
            {
              "identificador_comprovante": "%s",
              "data_hora_requisicao": "2026-07-08T20:03:57.116061100"
            }
            """.formatted(COMPROVANTE_ID);

    @Pact(consumer = "payment-service")
    V4Pact solicitacaoAceita(PactBuilder builder) {
        return builder
                .given("o serviço de Comprovantes está disponível")
                .expectsToReceiveHttpInteraction("uma solicitação válida de comprovante PIX", http -> http
                        .withRequest(req -> req
                                .method("POST")
                                .path("/comprovantes")
                                .header("Content-Type", "application/json")
                                .body(REQUEST_JSON, "application/json"))
                        .willRespondWith(res -> res
                                .status(202)
                                .header("Content-Type", "application/json")
                                .body(RESPONSE_202_JSON, "application/json")))
                .toPact();
    }

    @Pact(consumer = "payment-service")
    V4Pact comprovanteJaPersistido(PactBuilder builder) {
        return builder
                .given("um comprovante com id " + COMPROVANTE_ID + " já foi persistido")
                .expectsToReceiveHttpInteraction("uma consulta de confirmação de comprovante existente", http -> http
                        .withRequest(req -> req
                                .method("GET")
                                .path("/comprovantes/" + COMPROVANTE_ID))
                        .willRespondWith(res -> res.status(200)))
                .toPact();
    }

    @Pact(consumer = "payment-service")
    V4Pact comprovanteNaoEncontrado(PactBuilder builder) {
        return builder
                .given("nenhum comprovante existe com esse id")
                .expectsToReceiveHttpInteraction("uma consulta de confirmação de comprovante inexistente", http -> http
                        .withRequest(req -> req
                                .method("GET")
                                .path("/comprovantes/00000000-0000-0000-0000-000000000000"))
                        .willRespondWith(res -> res.status(404)))
                .toPact();
    }

    private ComprovanteHttpClient clienteApontandoPara(MockServer mockServer) {
        return new ComprovanteHttpClient(WebClientTestFactory.build(mockServer.getUrl()));
    }

    @Test
    @PactTestFor(pactMethod = "solicitacaoAceita")
    void solicitarComprovante_cumpreOContrato(MockServer mockServer) {
        SolicitacaoComprovante dados = new SolicitacaoComprovante(
                new DocumentoIdentificacao("Giovanni Vicente", TipoDocumento.CPF, "50329291076"),
                new ContaBancaria("2022", "00276", "0"),
                new ValorTransacao(new BigDecimal("23.99")),
                new DestinoPix(new ChavePix(TipoChavePix.CELULAR, "11948755536"), "Fernando Augusto"),
                "Segue pagamento da minha cota no churrasco de domingo",
                LocalDateTime.parse(DATA_HORA_TRANSACAO));

        ComprovanteSolicitado resultado = clienteApontandoPara(mockServer).solicitar(dados);

        assertThat(resultado.comprovanteId()).isEqualTo(UUID.fromString(COMPROVANTE_ID));
    }

    @Test
    @PactTestFor(pactMethod = "comprovanteJaPersistido")
    void confirmarPersistencia_comprovanteExistente_cumpreOContrato(MockServer mockServer) {
        boolean confirmado = clienteApontandoPara(mockServer)
                .confirmarPersistencia(UUID.fromString(COMPROVANTE_ID));

        assertThat(confirmado).isTrue();
    }

    @Test
    @PactTestFor(pactMethod = "comprovanteNaoEncontrado")
    void confirmarPersistencia_comprovanteInexistente_cumpreOContrato(MockServer mockServer) {
        boolean confirmado = clienteApontandoPara(mockServer)
                .confirmarPersistencia(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        assertThat(confirmado).isFalse();
    }
}
