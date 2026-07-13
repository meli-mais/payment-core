package com.projetocore.payment.application.usecase;

import com.projetocore.payment.application.saga.PagamentoSagaOrchestrator;
import com.projetocore.payment.domain.exception.IdempotencyKeyDuplicadaException;
import com.projetocore.payment.domain.model.Fatura;
import com.projetocore.payment.domain.port.FaturaRepositoryPort;
import com.projetocore.payment.domain.port.SolicitacaoComprovante;

/**
 * Caso de uso do endpoint {@code POST /api/v1/pagamentos}. Responsável pela checagem de
 * idempotência (FR-006 / US2) antes de acionar a saga.
 */
public class SolicitarPagamentoPixUseCase {

    private final FaturaRepositoryPort faturaRepository;
    private final PagamentoSagaOrchestrator sagaOrchestrator;

    public SolicitarPagamentoPixUseCase(FaturaRepositoryPort faturaRepository,
                                         PagamentoSagaOrchestrator sagaOrchestrator) {
        this.faturaRepository = faturaRepository;
        this.sagaOrchestrator = sagaOrchestrator;
    }

    public Fatura executar(String idempotencyKey, SolicitacaoComprovante dados) {
        return faturaRepository.buscarPorIdempotencyKey(idempotencyKey)
                .orElseGet(() -> criarEProcessar(idempotencyKey, dados));
    }

    private Fatura criarEProcessar(String idempotencyKey, SolicitacaoComprovante dados) {
        Fatura fatura = Fatura.receber(
                idempotencyKey, dados.documentoOrigem(), dados.contaOrigem(), dados.valorTransacao(),
                dados.destinoPix(), dados.identificacaoPix(), dados.dataHoraTransacao());
        try {
            faturaRepository.salvar(fatura);
        } catch (IdempotencyKeyDuplicadaException e) {
            // Requisição concorrente com a mesma chave venceu a corrida — retorna o resultado
            // dela em vez de propagar erro (spec.md, Edge Cases: "reenviada enquanto a saga
            // original ainda está em andamento").
            return faturaRepository.buscarPorIdempotencyKey(idempotencyKey).orElseThrow(() -> e);
        }
        sagaOrchestrator.processar(fatura);
        return fatura;
    }
}
