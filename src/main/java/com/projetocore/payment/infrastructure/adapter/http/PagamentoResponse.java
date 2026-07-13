package com.projetocore.payment.infrastructure.adapter.http;

import com.projetocore.payment.domain.model.Fatura;
import com.projetocore.payment.domain.model.StatusFatura;

import java.time.Instant;
import java.util.UUID;

/** Response de {@code POST /api/v1/pagamentos}. */
public record PagamentoResponse(UUID faturaId, StatusFatura status, Instant dataHoraRequisicao) {

    public static PagamentoResponse de(Fatura fatura) {
        return new PagamentoResponse(fatura.getId(), fatura.getStatus(), fatura.getCriadaEm());
    }
}
