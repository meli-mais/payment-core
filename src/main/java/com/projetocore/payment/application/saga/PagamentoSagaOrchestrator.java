package com.projetocore.payment.application.saga;

import com.projetocore.payment.domain.exception.ComprovanteIndisponivelException;
import com.projetocore.payment.domain.model.Fatura;
import com.projetocore.payment.domain.port.ComprovanteGatewayPort;
import com.projetocore.payment.domain.port.ComprovanteSolicitado;
import com.projetocore.payment.domain.port.FaturaRepositoryPort;
import com.projetocore.payment.domain.port.SolicitacaoComprovante;

import java.util.UUID;
import java.util.function.LongConsumer;

/**
 * Orquestrador da SAGA (constitution.md, Princípio II — estilo orquestração, não
 * coreografia). Conduz os dois passos definidos em research.md Decisão 1: solicitar o
 * comprovante e, em seguida, confirmar sua persistência via polling limitado — persistindo
 * a Fatura a cada transição de estado.
 */
public class PagamentoSagaOrchestrator {

    private final ComprovanteGatewayPort comprovanteGateway;
    private final FaturaRepositoryPort faturaRepository;
    private final int maxTentativasConfirmacao;
    private final long intervaloEntreTentativasMs;
    private final LongConsumer sleeper;

    public PagamentoSagaOrchestrator(ComprovanteGatewayPort comprovanteGateway,
                                      FaturaRepositoryPort faturaRepository,
                                      int maxTentativasConfirmacao,
                                      long intervaloEntreTentativasMs,
                                      LongConsumer sleeper) {
        this.comprovanteGateway = comprovanteGateway;
        this.faturaRepository = faturaRepository;
        this.maxTentativasConfirmacao = maxTentativasConfirmacao;
        this.intervaloEntreTentativasMs = intervaloEntreTentativasMs;
        this.sleeper = sleeper;
    }

    /** Executa a saga do início ao fim para uma Fatura recém-recebida (status RECEBIDA). */
    public void processar(Fatura fatura) {
        if (!passoUm_solicitarComprovante(fatura)) {
            return;
        }
        passoDois_confirmarPersistencia(fatura);
    }

    private boolean passoUm_solicitarComprovante(Fatura fatura) {
        SolicitacaoComprovante dados = new SolicitacaoComprovante(
                fatura.getDocumentoOrigem(), fatura.getContaOrigem(), fatura.getValorTransacao(),
                fatura.getDestinoPix(), fatura.getIdentificacaoPix(), fatura.getDataHoraTransacao());

        try {
            ComprovanteSolicitado resultado = comprovanteGateway.solicitar(dados);
            fatura.solicitarComprovante(resultado.comprovanteId());
            faturaRepository.salvar(fatura);
            return true;
        } catch (ComprovanteIndisponivelException e) {
            fatura.falhar("Comprovantes indisponível ao solicitar: " + e.getMessage());
            faturaRepository.salvar(fatura);
            return false;
        }
    }

    private void passoDois_confirmarPersistencia(Fatura fatura) {
        for (int tentativa = 1; tentativa <= maxTentativasConfirmacao; tentativa++) {
            fatura.registrarTentativaConfirmacao();

            if (confirmado(fatura.getComprovanteId())) {
                fatura.confirmarPagamento();
                faturaRepository.salvar(fatura);
                return;
            }

            if (tentativa < maxTentativasConfirmacao) {
                sleeper.accept(intervaloEntreTentativasMs);
            }
        }

        fatura.falhar("Confirmação de persistência do comprovante esgotada após "
                + maxTentativasConfirmacao + " tentativas");
        faturaRepository.salvar(fatura);
    }

    /**
     * Uma falha técnica (5xx, timeout) ao consultar a confirmação conta como "não
     * confirmado nesta tentativa", não como erro fatal — o loop limitado acima já é a
     * política de retry para esse caso (constitution.md, Princípio IV).
     */
    private boolean confirmado(UUID comprovanteId) {
        try {
            return comprovanteGateway.confirmarPersistencia(comprovanteId);
        } catch (ComprovanteIndisponivelException e) {
            return false;
        }
    }
}
