package com.projetocore.payment.application.saga;

import com.projetocore.payment.application.support.FakeComprovanteGateway;
import com.projetocore.payment.application.support.FakeFaturaRepository;
import com.projetocore.payment.domain.model.*;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes da orquestração da SAGA — US1 (sucesso) e US3 (compensação), usando fakes das
 * portas do domínio. Nenhuma dependência de Spring/HTTP/JPA (constitution.md, Princípio III).
 */
class PagamentoSagaOrchestratorTest {

    private static final int MAX_TENTATIVAS_CONFIRMACAO = 3;

    private Fatura novaFaturaRecebida(FakeFaturaRepository repositorio) {
        Fatura fatura = Fatura.receber(
                "demo-" + System.nanoTime(),
                new DocumentoIdentificacao("Giovanni Vicente", TipoDocumento.CPF, "50329291076"),
                new ContaBancaria("2022", "00276", "0"),
                new ValorTransacao(new BigDecimal("23.99")),
                new DestinoPix(new ChavePix(TipoChavePix.CELULAR, "11948755536"), "Fernando Augusto"),
                "Segue pagamento da minha cota no churrasco de domingo",
                LocalDateTime.now()
        );
        repositorio.salvar(fatura);
        return fatura;
    }

    private PagamentoSagaOrchestrator orquestrador(FakeComprovanteGateway gateway, FakeFaturaRepository repositorio) {
        return new PagamentoSagaOrchestrator(gateway, repositorio, MAX_TENTATIVAS_CONFIRMACAO, 0, millis -> {
        });
    }

    @Test
    void caminhoFeliz_comprovanteConfirmadoNaPrimeiraTentativa_faturaVaiParaPaga() {
        FakeFaturaRepository repositorio = new FakeFaturaRepository();
        FakeComprovanteGateway gateway = FakeComprovanteGateway.confirmaImediatamente();
        Fatura fatura = novaFaturaRecebida(repositorio);

        orquestrador(gateway, repositorio).processar(fatura);

        assertThat(fatura.getStatus()).isEqualTo(StatusFatura.PAGA);
        assertThat(fatura.getComprovanteId()).isNotNull();
        assertThat(gateway.chamadasSolicitar()).isEqualTo(1);
    }

    @Test
    void comprovantesRejeitaSolicitacao_faturaVaiParaFalhouSemComprovanteId() {
        FakeFaturaRepository repositorio = new FakeFaturaRepository();
        FakeComprovanteGateway gateway = FakeComprovanteGateway.rejeitaSolicitacao();
        Fatura fatura = novaFaturaRecebida(repositorio);

        orquestrador(gateway, repositorio).processar(fatura);

        assertThat(fatura.getStatus()).isEqualTo(StatusFatura.FALHOU);
        assertThat(fatura.getComprovanteId()).isNull();
        assertThat(fatura.getMotivoFalha()).isNotBlank();
        assertThat(gateway.chamadasConfirmar()).isZero();
    }

    @Test
    void confirmacaoNuncaChega_esgotaTentativasEFalhaRetendoComprovanteIdParaReconciliacao() {
        FakeFaturaRepository repositorio = new FakeFaturaRepository();
        FakeComprovanteGateway gateway = FakeComprovanteGateway.nuncaConfirma();
        Fatura fatura = novaFaturaRecebida(repositorio);

        orquestrador(gateway, repositorio).processar(fatura);

        assertThat(fatura.getStatus()).isEqualTo(StatusFatura.FALHOU);
        assertThat(fatura.getComprovanteId()).isNotNull(); // retido para reconciliação — FR-008
        assertThat(fatura.getMotivoFalha()).isNotBlank();
        assertThat(gateway.chamadasConfirmar()).isEqualTo(MAX_TENTATIVAS_CONFIRMACAO);
    }

    @Test
    void cadaTransicaoEPersistidaAoLongoDaSaga() {
        FakeFaturaRepository repositorio = new FakeFaturaRepository();
        FakeComprovanteGateway gateway = FakeComprovanteGateway.confirmaImediatamente();
        Fatura fatura = novaFaturaRecebida(repositorio);

        orquestrador(gateway, repositorio).processar(fatura);

        Fatura recarregada = repositorio.buscarPorId(fatura.getId()).orElseThrow();
        assertThat(recarregada.getStatus()).isEqualTo(StatusFatura.PAGA);
    }
}
