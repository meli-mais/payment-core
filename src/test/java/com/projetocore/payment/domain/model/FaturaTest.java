package com.projetocore.payment.domain.model;

import com.projetocore.payment.domain.event.FaturaFalhou;
import com.projetocore.payment.domain.event.FaturaPaga;
import com.projetocore.payment.domain.exception.TransicaoInvalidaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Máquina de estados da Fatura — US1 (pagamento feliz) e US3 (compensação).
 */
class FaturaTest {

    private Fatura novaFatura(String idempotencyKey) {
        return Fatura.receber(
                idempotencyKey,
                new DocumentoIdentificacao("Giovanni Vicente", TipoDocumento.CPF, "50329291076"),
                new ContaBancaria("2022", "00276", "0"),
                new ValorTransacao(new BigDecimal("23.99")),
                new DestinoPix(new ChavePix(TipoChavePix.CELULAR, "11948755536"), "Fernando Augusto"),
                "Segue pagamento da minha cota no churrasco de domingo",
                LocalDateTime.parse("2026-07-08T20:03:57.116061100")
        );
    }

    @Test
    void novaFaturaComecaEmRecebida() {
        Fatura fatura = novaFatura("demo-001");
        assertThat(fatura.getStatus()).isEqualTo(StatusFatura.RECEBIDA);
        assertThat(fatura.getComprovanteId()).isNull();
        assertThat(fatura.getTentativasConfirmacao()).isZero();
    }

    @Test
    void solicitarComprovanteTransitaParaComprovanteSolicitado() {
        Fatura fatura = novaFatura("demo-001");
        UUID comprovanteId = UUID.randomUUID();

        fatura.solicitarComprovante(comprovanteId);

        assertThat(fatura.getStatus()).isEqualTo(StatusFatura.COMPROVANTE_SOLICITADO);
        assertThat(fatura.getComprovanteId()).isEqualTo(comprovanteId);
    }

    @Test
    void confirmarPagamentoTransitaParaPagaERaisesEvento() {
        Fatura fatura = novaFatura("demo-001");
        UUID comprovanteId = UUID.randomUUID();
        fatura.solicitarComprovante(comprovanteId);

        fatura.confirmarPagamento();

        assertThat(fatura.getStatus()).isEqualTo(StatusFatura.PAGA);
        assertThat(fatura.pullEventos())
                .singleElement()
                .isInstanceOf(FaturaPaga.class)
                .extracting("comprovanteId")
                .isEqualTo(comprovanteId);
    }

    @Test
    void naoPodeConfirmarPagamentoSemTerSolicitadoComprovante() {
        Fatura fatura = novaFatura("demo-001");

        assertThatThrownBy(fatura::confirmarPagamento)
                .isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void falharAPartirDeRecebidaTransitaParaFalhouComMotivoERaisesEvento() {
        Fatura fatura = novaFatura("demo-001");

        fatura.falhar("Comprovantes rejeitou a solicitação (circuit breaker aberto)");

        assertThat(fatura.getStatus()).isEqualTo(StatusFatura.FALHOU);
        assertThat(fatura.getMotivoFalha()).isNotBlank();
        assertThat(fatura.pullEventos()).singleElement().isInstanceOf(FaturaFalhou.class);
    }

    @Test
    void falharAPartirDeComprovanteSolicitadoRetemComprovanteIdParaReconciliacao() {
        Fatura fatura = novaFatura("demo-001");
        UUID comprovanteId = UUID.randomUUID();
        fatura.solicitarComprovante(comprovanteId);

        fatura.falhar("Confirmação por GET esgotada após tentativas máximas");

        assertThat(fatura.getStatus()).isEqualTo(StatusFatura.FALHOU);
        assertThat(fatura.getComprovanteId()).isEqualTo(comprovanteId);
    }

    @Test
    void naoPodeFalharSemMotivo() {
        Fatura fatura = novaFatura("demo-001");

        assertThatThrownBy(() -> fatura.falhar(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> fatura.falhar(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void naoPodeFalharUmaFaturaJaPaga() {
        Fatura fatura = novaFatura("demo-001");
        fatura.solicitarComprovante(UUID.randomUUID());
        fatura.confirmarPagamento();

        assertThatThrownBy(() -> fatura.falhar("tentativa tardia"))
                .isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void naoPodeSolicitarComprovanteDuasVezes() {
        Fatura fatura = novaFatura("demo-001");
        fatura.solicitarComprovante(UUID.randomUUID());

        assertThatThrownBy(() -> fatura.solicitarComprovante(UUID.randomUUID()))
                .isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void registrarTentativaConfirmacaoIncrementaContador() {
        Fatura fatura = novaFatura("demo-001");
        fatura.solicitarComprovante(UUID.randomUUID());

        fatura.registrarTentativaConfirmacao();
        fatura.registrarTentativaConfirmacao();

        assertThat(fatura.getTentativasConfirmacao()).isEqualTo(2);
    }

    @Test
    void rejeitaIdempotencyKeyAusente() {
        assertThatThrownBy(() -> Fatura.receber(
                "",
                new DocumentoIdentificacao("Giovanni Vicente", TipoDocumento.CPF, "50329291076"),
                new ContaBancaria("2022", "00276", "0"),
                new ValorTransacao(new BigDecimal("23.99")),
                new DestinoPix(new ChavePix(TipoChavePix.CELULAR, "11948755536"), "Fernando Augusto"),
                null,
                LocalDateTime.now()
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
