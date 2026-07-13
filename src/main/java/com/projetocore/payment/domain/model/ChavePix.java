package com.projetocore.payment.domain.model;

import java.util.Objects;

public record ChavePix(TipoChavePix tipo, String valor) {

    public ChavePix {
        Objects.requireNonNull(tipo, "tipo_chave_pix_destino é obrigatório");
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("chave_pix_destino é obrigatório");
        }
    }
}
