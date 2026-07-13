package com.projetocore.payment.application.usecase;

import com.projetocore.payment.domain.model.Fatura;
import com.projetocore.payment.domain.port.FaturaRepositoryPort;

import java.util.Optional;
import java.util.UUID;

/** Caso de uso do endpoint {@code GET /api/v1/pagamentos/{id}}. */
public class ConsultarFaturaUseCase {

    private final FaturaRepositoryPort faturaRepository;

    public ConsultarFaturaUseCase(FaturaRepositoryPort faturaRepository) {
        this.faturaRepository = faturaRepository;
    }

    public Optional<Fatura> executar(UUID faturaId) {
        return faturaRepository.buscarPorId(faturaId);
    }
}
