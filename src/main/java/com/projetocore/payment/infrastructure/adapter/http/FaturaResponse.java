package com.projetocore.payment.infrastructure.adapter.http;

import com.projetocore.payment.domain.model.Fatura;
import com.projetocore.payment.domain.model.StatusFatura;

import java.time.Instant;
import java.util.UUID;

/** Response de {@code GET /api/v1/pagamentos/{id}}. */
public record FaturaResponse(UUID faturaId, StatusFatura status, UUID comprovanteId,
                              String motivoFalha, Instant criadaEm, Instant atualizadaEm) {

    public static FaturaResponse de(Fatura fatura) {
        return new FaturaResponse(fatura.getId(), fatura.getStatus(), fatura.getComprovanteId(),
                fatura.getMotivoFalha(), fatura.getCriadaEm(), fatura.getAtualizadaEm());
    }
}
