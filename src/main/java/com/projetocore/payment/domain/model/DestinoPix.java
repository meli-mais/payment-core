package com.projetocore.payment.domain.model;

import java.util.Objects;

/**
 * Value Object para o destinatário do PIX: chave + {@code nome_cliente_destino}.
 */
public record DestinoPix(ChavePix chave, String nomeClienteDestino) {

    public DestinoPix {
        Objects.requireNonNull(chave, "chave PIX de destino é obrigatória");
        if (nomeClienteDestino == null || nomeClienteDestino.isBlank()) {
            throw new IllegalArgumentException("nome_cliente_destino é obrigatório");
        }
    }
}
