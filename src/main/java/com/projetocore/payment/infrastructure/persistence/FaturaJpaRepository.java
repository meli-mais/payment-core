package com.projetocore.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FaturaJpaRepository extends JpaRepository<FaturaJpaEntity, UUID> {

    Optional<FaturaJpaEntity> findByIdempotencyKey(String idempotencyKey);
}
