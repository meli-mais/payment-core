package com.projetocore.payment.infrastructure.persistence;

import com.projetocore.payment.domain.exception.IdempotencyKeyDuplicadaException;
import com.projetocore.payment.domain.model.Fatura;
import com.projetocore.payment.domain.port.FaturaRepositoryPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class FaturaRepositoryAdapter implements FaturaRepositoryPort {

    private final FaturaJpaRepository jpaRepository;

    public FaturaRepositoryAdapter(FaturaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Fatura salvar(Fatura fatura) {
        try {
            FaturaJpaEntity salva = jpaRepository.save(FaturaEntityMapper.paraEntidade(fatura));
            return FaturaEntityMapper.paraDominio(salva);
        } catch (DataIntegrityViolationException e) {
            // Corrida entre duas requisições com a mesma Idempotency-Key — a constraint única
            // do banco pegou o que a checagem em memória (com sua janela de corrida) não pega.
            throw new IdempotencyKeyDuplicadaException(fatura.getIdempotencyKey(), e);
        }
    }

    @Override
    public Optional<Fatura> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(FaturaEntityMapper::paraDominio);
    }

    @Override
    public Optional<Fatura> buscarPorIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(FaturaEntityMapper::paraDominio);
    }
}
