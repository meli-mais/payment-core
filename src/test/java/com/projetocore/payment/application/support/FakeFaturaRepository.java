package com.projetocore.payment.application.support;

import com.projetocore.payment.domain.exception.IdempotencyKeyDuplicadaException;
import com.projetocore.payment.domain.model.Fatura;
import com.projetocore.payment.domain.port.FaturaRepositoryPort;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação em memória de {@link FaturaRepositoryPort} para testes de Application,
 * sem nenhuma dependência de Spring/JPA (constitution.md, Princípio I). Reproduz a constraint
 * única de {@code idempotencyKey} do banco real, para permitir testar a corrida entre
 * requisições concorrentes (ver {@link IdempotencyKeyDuplicadaException}).
 */
public class FakeFaturaRepository implements FaturaRepositoryPort {

    private final Map<UUID, Fatura> porId = new LinkedHashMap<>();

    @Override
    public Fatura salvar(Fatura fatura) {
        boolean chaveJaUsadaPorOutraFatura = porId.values().stream()
                .anyMatch(f -> !f.getId().equals(fatura.getId())
                        && f.getIdempotencyKey().equals(fatura.getIdempotencyKey()));
        if (chaveJaUsadaPorOutraFatura) {
            throw new IdempotencyKeyDuplicadaException(fatura.getIdempotencyKey(), null);
        }
        porId.put(fatura.getId(), fatura);
        return fatura;
    }

    @Override
    public Optional<Fatura> buscarPorId(UUID id) {
        return Optional.ofNullable(porId.get(id));
    }

    @Override
    public Optional<Fatura> buscarPorIdempotencyKey(String idempotencyKey) {
        return porId.values().stream()
                .filter(f -> f.getIdempotencyKey().equals(idempotencyKey))
                .findFirst();
    }
}
