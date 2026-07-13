package com.projetocore.payment.application.usecase;

import com.projetocore.payment.application.saga.PagamentoSagaOrchestrator;
import com.projetocore.payment.application.support.FakeComprovanteGateway;
import com.projetocore.payment.application.support.FakeFaturaRepository;
import com.projetocore.payment.domain.model.*;
import com.projetocore.payment.domain.port.FaturaRepositoryPort;
import com.projetocore.payment.domain.port.SolicitacaoComprovante;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Idempotência (US2) — reenviar a mesma Idempotency-Key não deve reiniciar a saga nem
 * chamar o Comprovantes de novo (FR-006).
 */
class SolicitarPagamentoPixUseCaseTest {

    private SolicitacaoComprovante dadosPadrao() {
        return new SolicitacaoComprovante(
                new DocumentoIdentificacao("Giovanni Vicente", TipoDocumento.CPF, "50329291076"),
                new ContaBancaria("2022", "00276", "0"),
                new ValorTransacao(new BigDecimal("23.99")),
                new DestinoPix(new ChavePix(TipoChavePix.CELULAR, "11948755536"), "Fernando Augusto"),
                "Segue pagamento da minha cota no churrasco de domingo",
                LocalDateTime.now()
        );
    }

    private SolicitarPagamentoPixUseCase useCase(FakeFaturaRepository repositorio, FakeComprovanteGateway gateway) {
        PagamentoSagaOrchestrator orquestrador =
                new PagamentoSagaOrchestrator(gateway, repositorio, 3, 0, millis -> {
                });
        return new SolicitarPagamentoPixUseCase(repositorio, orquestrador);
    }

    @Test
    void primeiraRequisicaoCriaFaturaEExecutaSaga() {
        FakeFaturaRepository repositorio = new FakeFaturaRepository();
        FakeComprovanteGateway gateway = FakeComprovanteGateway.confirmaImediatamente();

        Fatura fatura = useCase(repositorio, gateway).executar("idem-001", dadosPadrao());

        assertThat(fatura.getStatus()).isEqualTo(StatusFatura.PAGA);
        assertThat(gateway.chamadasSolicitar()).isEqualTo(1);
    }

    @Test
    void reenvioComMesmaIdempotencyKeyRetornaResultadoOriginalSemChamarComprovantesDeNovo() {
        FakeFaturaRepository repositorio = new FakeFaturaRepository();
        FakeComprovanteGateway gateway = FakeComprovanteGateway.confirmaImediatamente();
        SolicitarPagamentoPixUseCase useCase = useCase(repositorio, gateway);

        Fatura primeira = useCase.executar("idem-001", dadosPadrao());
        Fatura segunda = useCase.executar("idem-001", dadosPadrao());

        assertThat(segunda.getId()).isEqualTo(primeira.getId());
        assertThat(gateway.chamadasSolicitar()).isEqualTo(1); // não chamou de novo
    }

    @Test
    void requisicoesComIdempotencyKeysDiferentesCriamFaturasIndependentes() {
        FakeFaturaRepository repositorio = new FakeFaturaRepository();
        FakeComprovanteGateway gateway = FakeComprovanteGateway.confirmaImediatamente();
        SolicitarPagamentoPixUseCase useCase = useCase(repositorio, gateway);

        Fatura primeira = useCase.executar("idem-001", dadosPadrao());
        Fatura segunda = useCase.executar("idem-002", dadosPadrao());

        assertThat(segunda.getId()).isNotEqualTo(primeira.getId());
        assertThat(gateway.chamadasSolicitar()).isEqualTo(2);
    }

    /**
     * Simula duas requisições concorrentes com a mesma Idempotency-Key: ambas passam pela
     * checagem "não existe ainda" antes de uma delas conseguir salvar — a constraint única do
     * banco (aqui, simulada) pega a segunda. O caso de uso deve devolver o resultado da
     * vencedora, não propagar erro (spec.md, Edge Cases).
     */
    @Test
    void duasRequisicoesConcorrentesComMesmaIdempotencyKey_naoPropagaErro_devolveResultadoDaVencedora() {
        FakeFaturaRepository delegate = new FakeFaturaRepository();
        FakeComprovanteGateway gateway = FakeComprovanteGateway.confirmaImediatamente();

        Fatura vencedora = Fatura.receber("idem-concorrente",
                new DocumentoIdentificacao("Giovanni Vicente", TipoDocumento.CPF, "50329291076"),
                new ContaBancaria("2022", "00276", "0"),
                new ValorTransacao(new BigDecimal("23.99")),
                new DestinoPix(new ChavePix(TipoChavePix.CELULAR, "11948755536"), "Fernando Augusto"),
                "outra requisição venceu a corrida", LocalDateTime.now());

        FaturaRepositoryPort repositorioComCorrida = new FaturaRepositoryPort() {
            private boolean primeiraChamada = true;

            @Override
            public Fatura salvar(Fatura fatura) {
                if (primeiraChamada) {
                    primeiraChamada = false;
                    delegate.salvar(vencedora); // "outra requisição" grava primeiro
                }
                return delegate.salvar(fatura); // agora sim colide de verdade
            }

            @Override
            public Optional<Fatura> buscarPorId(UUID id) {
                return delegate.buscarPorId(id);
            }

            @Override
            public Optional<Fatura> buscarPorIdempotencyKey(String idempotencyKey) {
                return delegate.buscarPorIdempotencyKey(idempotencyKey);
            }
        };

        PagamentoSagaOrchestrator orquestrador =
                new PagamentoSagaOrchestrator(gateway, repositorioComCorrida, 3, 0, millis -> {
                });
        SolicitarPagamentoPixUseCase useCase = new SolicitarPagamentoPixUseCase(repositorioComCorrida, orquestrador);

        Fatura resultado = useCase.executar("idem-concorrente", dadosPadrao());

        assertThat(resultado.getId()).isEqualTo(vencedora.getId());
        assertThat(gateway.chamadasSolicitar()).isZero(); // a saga da "perdedora" nunca rodou
    }
}
