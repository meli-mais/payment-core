package com.projetocore.payment.domain.port;

import com.projetocore.payment.domain.model.Fatura;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para persistência da Fatura. Implementação real fica em
 * infrastructure.persistence (constitution.md, Princípio I).
 */
public interface FaturaRepositoryPort {

    Fatura salvar(Fatura fatura);

    Optional<Fatura> buscarPorId(UUID id);

    Optional<Fatura> buscarPorIdempotencyKey(String idempotencyKey);
}
