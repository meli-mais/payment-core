package com.projetocore.payment.domain.port;

import com.projetocore.payment.domain.exception.ComprovanteIndisponivelException;

import java.util.UUID;

/**
 * Porta de saída para o microsserviço de Comprovantes (dependência externa — Membro 2/3,
 * fora deste repositório). A implementação real chama HTTP via WireMock/real endpoint,
 * protegida por Resilience4j (constitution.md, Princípios IV e V).
 */
public interface ComprovanteGatewayPort {

    /**
     * Passo 1 da saga: {@code POST /comprovantes}.
     *
     * @throws ComprovanteIndisponivelException se o Comprovantes rejeitar a solicitação ou
     *         a política de resiliência se esgotar — sinal para a saga compensar (FR-005).
     */
    ComprovanteSolicitado solicitar(SolicitacaoComprovante dados);

    /**
     * Confirmação de persistência: {@code GET /comprovantes/{id}}.
     *
     * @return {@code true} se o comprovante foi encontrado (200 — confirmado),
     *         {@code false} se ainda não foi encontrado nesta tentativa (404 — não confirmado
     *         ainda, não necessariamente falha definitiva; ver PagamentoSagaOrchestrator).
     */
    boolean confirmarPersistencia(UUID comprovanteId);
}
