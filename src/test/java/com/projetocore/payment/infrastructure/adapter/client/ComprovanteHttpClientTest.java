package com.projetocore.payment.infrastructure.adapter.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.projetocore.payment.domain.exception.ComprovanteIndisponivelException;
import com.projetocore.payment.domain.model.*;
import com.projetocore.payment.domain.port.ComprovanteSolicitado;
import com.projetocore.payment.domain.port.SolicitacaoComprovante;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste de integração contra WireMock real (via {@link WireMockExtension}, sem Docker — ver
 * research.md Decisão 3.1). As mappings vêm de {@code wiremock/mappings/} na raiz do
 * repositório — nunca duplicadas inline (constitution.md, Princípio V).
 */
class ComprovanteHttpClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().usingFilesUnderDirectory("wiremock").dynamicPort())
            .build();

    private ComprovanteHttpClient client() {
        return new ComprovanteHttpClient(WebClientTestFactory.build(wireMock.baseUrl()));
    }

    private SolicitacaoComprovante dados(String numeroDocumento) {
        return new SolicitacaoComprovante(
                new DocumentoIdentificacao("Giovanni Vicente", TipoDocumento.CPF, numeroDocumento),
                new ContaBancaria("2022", "00276", "0"),
                new ValorTransacao(new BigDecimal("23.99")),
                new DestinoPix(new ChavePix(TipoChavePix.CELULAR, "11948755536"), "Fernando Augusto"),
                "Segue pagamento da minha cota no churrasco de domingo",
                LocalDateTime.parse("2026-07-08T20:03:57.116061100")
        );
    }

    @Test
    void solicitarComDocumentoPadrao_retornaComprovanteAceito() {
        ComprovanteSolicitado resultado = client().solicitar(dados("50329291076"));

        assertThat(resultado.comprovanteId())
                .isEqualTo(UUID.fromString("b819cc65-f6f0-478c-8bb7-69ee1c4f6402"));
    }

    @Test
    void confirmarPersistenciaDoComprovanteConhecido_retornaTrue() {
        boolean confirmado = client()
                .confirmarPersistencia(UUID.fromString("b819cc65-f6f0-478c-8bb7-69ee1c4f6402"));

        assertThat(confirmado).isTrue();
    }

    @Test
    void confirmarPersistenciaDeComprovanteDesconhecido_retornaFalse() {
        boolean confirmado = client().confirmarPersistencia(UUID.randomUUID());

        assertThat(confirmado).isFalse();
    }

    @Test
    void solicitarComDocumentoSentinelaDeErro_lancaComprovanteIndisponivel() {
        assertThatThrownBy(() -> client().solicitar(dados("00000000000")))
                .isInstanceOf(ComprovanteIndisponivelException.class);
    }

    @Test
    void solicitarComDocumentoSentinelaDeComprovanteFantasma_aceitaMasNuncaConfirma() {
        ComprovanteSolicitado resultado = client().solicitar(dados("11111111111"));
        assertThat(resultado.comprovanteId())
                .isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        boolean confirmado = client().confirmarPersistencia(resultado.comprovanteId());

        assertThat(confirmado).isFalse();
    }

    @Test
    void solicitarComRespostaContendoUuidInvalido_lancaComprovanteIndisponivelNaoErroDeValidacao() {
        assertThatThrownBy(() -> client().solicitar(dados("22222222222")))
                .isInstanceOf(ComprovanteIndisponivelException.class);
    }
}
